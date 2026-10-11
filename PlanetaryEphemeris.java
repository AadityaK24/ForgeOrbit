import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Approximate heliocentric planetary positions in the J2000 ecliptic frame.
 * The orbital elements and long-range correction terms follow JPL SSD's
 * compact Keplerian model. This is not a high-precision flight ephemeris.
 */
public class PlanetaryEphemeris
{
    private static final double MIN_CENTURIES_FROM_J2000 = -50.0; // 3000 BC
    private static final double MAX_CENTURIES_FROM_J2000 = 10.0;  // 3000 AD
    private static final double VELOCITY_STEP_DAYS = 0.001;

    // Compatibility helpers for older mission-planning code.
    public static CelestialBody getBody(String bodyName)
    {
        return SolarSystemCatalog.getBody(bodyName);
    }

    public static double getHeliocentricLongitudeRadians(String bodyName, LocalDateTime dateTime)
    {
        if (dateTime == null)
            throw new IllegalArgumentException("Date and time cannot be null.");
        double jd = julianDate(dateTime);
        Vector3 position = new PlanetaryEphemeris().calculateState(bodyName, jd).getPosition();
        return normalizeAngle(Math.atan2(position.getY(), position.getX()));
    }

    public static Vector3 getHeliocentricPosition(String bodyName, LocalDateTime dateTime)
    {
        if (dateTime == null)
            throw new IllegalArgumentException("Date and time cannot be null.");
        return new PlanetaryEphemeris().calculateState(bodyName, julianDate(dateTime)).getPosition();
    }

    public static double getPhaseAngleRadians(String origin, String destination, LocalDateTime dateTime)
    {
        return normalizeAngle(getHeliocentricLongitudeRadians(destination, dateTime)
            - getHeliocentricLongitudeRadians(origin, dateTime));
    }

    public static double normalizeAngle(double angle)
    {
        double value = angle % Constants.TWO_PI;
        if (value < 0.0) value += Constants.TWO_PI;
        return value;
    }

    private static double julianDate(LocalDateTime dateTime)
    {
        LocalDateTime epoch = LocalDateTime.of(2000, 1, 1, 12, 0);
        double days = Duration.between(epoch, dateTime).toMillis() / 86400000.0;
        return Constants.J2000_JULIAN_DATE + days;
    }

    public PlanetaryState calculateState(String bodyName, double julianDate)
    {
        return calculateState(SolarSystemCatalog.getBody(bodyName), julianDate);
    }

    public PlanetaryState calculateState(CelestialBody body, double julianDate)
    {
        if (body == null)
            throw new IllegalArgumentException("Celestial body cannot be null.");
        validateDate(julianDate);

        Vector3 position = calculatePosition(body, julianDate);
        if (body.isStar() || body.getOrbitalElements() == null)
            return new PlanetaryState(body, julianDate, position, new Vector3(0.0, 0.0, 0.0));

        double beforeDate = Math.max(minimumJulianDate(), julianDate - VELOCITY_STEP_DAYS);
        double afterDate = Math.min(maximumJulianDate(), julianDate + VELOCITY_STEP_DAYS);
        Vector3 before = calculatePosition(body, beforeDate);
        Vector3 after = calculatePosition(body, afterDate);
        double elapsedSeconds = (afterDate - beforeDate) * Constants.SECONDS_PER_DAY;
        Vector3 velocity = after.subtract(before).scale(1.0 / elapsedSeconds);
        return new PlanetaryState(body, julianDate, position, velocity);
    }

    public List<PlanetaryState> calculateAll(double julianDate)
    {
        validateDate(julianDate);
        List<PlanetaryState> states = new ArrayList<PlanetaryState>();
        for (CelestialBody body : SolarSystemCatalog.getAllBodies())
            states.add(calculateState(body, julianDate));
        return Collections.unmodifiableList(states);
    }

    private Vector3 calculatePosition(CelestialBody body, double julianDate)
    {
        validateDate(julianDate);
        OrbitalElements elements = body.getOrbitalElements();
        if (body.isStar())
            return new Vector3(0.0, 0.0, 0.0);
        if (elements == null)
            throw new IllegalArgumentException("No orbital elements are available for " + body.getName() + ".");

        double centuries = (julianDate - Constants.J2000_JULIAN_DATE) / 36525.0;
        double semiMajorAxis = elements.semiMajorAxisAuAt(centuries) * Constants.AU_TO_M;
        double eccentricity = elements.eccentricityAt(centuries);
        double inclination = Math.toRadians(elements.inclinationDegreesAt(centuries));
        double meanLongitude = elements.meanLongitudeDegreesAt(centuries);
        double longitudePerihelion = elements.longitudePerihelionDegreesAt(centuries);
        double ascendingNode = elements.ascendingNodeDegreesAt(centuries);

        if (!Double.isFinite(semiMajorAxis) || semiMajorAxis <= 0.0 || eccentricity < 0.0 || eccentricity >= 1.0)
            throw new ArithmeticException("Orbital elements became invalid at the selected date.");

        double correction = elements.getB() * centuries * centuries
            + elements.getC() * Math.cos(Math.toRadians(elements.getF() * centuries))
            + elements.getS() * Math.sin(Math.toRadians(elements.getF() * centuries));
        double meanAnomaly = Math.toRadians(normalizeDegrees(
            meanLongitude - longitudePerihelion + correction));
        double eccentricAnomaly = solveKepler(meanAnomaly, eccentricity);

        double xPrime = semiMajorAxis * (Math.cos(eccentricAnomaly) - eccentricity);
        double yPrime = semiMajorAxis * Math.sqrt(1.0 - eccentricity * eccentricity)
            * Math.sin(eccentricAnomaly);

        double argumentOfPerihelion = Math.toRadians(longitudePerihelion - ascendingNode);
        double node = Math.toRadians(ascendingNode);
        double cosW = Math.cos(argumentOfPerihelion);
        double sinW = Math.sin(argumentOfPerihelion);
        double cosN = Math.cos(node);
        double sinN = Math.sin(node);
        double cosI = Math.cos(inclination);
        double sinI = Math.sin(inclination);

        double x = (cosW * cosN - sinW * sinN * cosI) * xPrime
            + (-sinW * cosN - cosW * sinN * cosI) * yPrime;
        double y = (cosW * sinN + sinW * cosN * cosI) * xPrime
            + (-sinW * sinN + cosW * cosN * cosI) * yPrime;
        double z = (sinW * sinI) * xPrime + (cosW * sinI) * yPrime;
        return new Vector3(x, y, z);
    }

    private static double solveKepler(double meanAnomaly, double eccentricity)
    {
        double m = normalizeRadians(meanAnomaly);
        double eccentricAnomaly = m + eccentricity * Math.sin(m);
        for (int i = 0; i < 100; i++)
        {
            double denominator = 1.0 - eccentricity * Math.cos(eccentricAnomaly);
            double change = (eccentricAnomaly - eccentricity * Math.sin(eccentricAnomaly) - m) / denominator;
            eccentricAnomaly -= change;
            if (Math.abs(change) < 1.0e-13)
                return eccentricAnomaly;
        }
        throw new ArithmeticException("Kepler equation did not converge.");
    }

    private static double normalizeDegrees(double angle)
    {
        double result = angle % 360.0;
        if (result > 180.0) result -= 360.0;
        if (result < -180.0) result += 360.0;
        return result;
    }

    private static double normalizeRadians(double angle)
    {
        double result = angle % Constants.TWO_PI;
        if (result > Math.PI) result -= Constants.TWO_PI;
        if (result < -Math.PI) result += Constants.TWO_PI;
        return result;
    }

    private static void validateDate(double julianDate)
    {
        if (!Double.isFinite(julianDate))
            throw new IllegalArgumentException("Julian date must be finite.");
        double centuries = (julianDate - Constants.J2000_JULIAN_DATE) / 36525.0;
        if (centuries < MIN_CENTURIES_FROM_J2000 || centuries > MAX_CENTURIES_FROM_J2000)
            throw new IllegalArgumentException("This approximate planetary model is supported from 3000 BC to 3000 AD.");
    }

    private static double minimumJulianDate()
    {
        return Constants.J2000_JULIAN_DATE + MIN_CENTURIES_FROM_J2000 * 36525.0;
    }

    private static double maximumJulianDate()
    {
        return Constants.J2000_JULIAN_DATE + MAX_CENTURIES_FROM_J2000 * 36525.0;
    }
}
