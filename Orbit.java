public class Orbit
{
    private final CelestialBody centralBody;
    private double altitude;
    private double radius;

    public Orbit(double altitude)
    {
        this(new Earth(), altitude);
    }

    public Orbit(Earth earth, double altitude)
    {
        this((CelestialBody) earth, altitude);
    }

    public Orbit(CelestialBody centralBody, double altitude)
    {
        if (centralBody == null)
            throw new IllegalArgumentException("Central body cannot be null.");
        validateAltitude(centralBody, altitude);
        this.centralBody = centralBody;
        this.altitude = altitude;
        this.radius = centralBody.getRadius() + altitude;
    }

    /** Legacy accessor retained for Earth-orbit code. */
    public Earth getEarth()
    {
        if (!(centralBody instanceof Earth))
            throw new IllegalStateException("This orbit is not centred on the legacy Earth model.");
        return (Earth) centralBody;
    }

    public CelestialBody getCentralBody() { return centralBody; }
    public double getAltitude() { return altitude; }
    public double getRadius() { return radius; }

    public void setAltitude(double altitude)
    {
        validateAltitude(centralBody, altitude);
        double computedRadius = centralBody.getRadius() + altitude;
        this.altitude = altitude;
        this.radius = computedRadius;
    }

    private static void validateAltitude(CelestialBody centralBody, double altitude)
    {
        if (!Double.isFinite(altitude) || altitude <= 0.0)
            throw new IllegalArgumentException("Orbit altitude must be finite and positive.");
        double computedRadius = centralBody.getRadius() + altitude;
        if (!Double.isFinite(computedRadius) || computedRadius <= centralBody.getRadius())
            throw new IllegalArgumentException("Computed orbital radius is invalid.");
    }

    public double getAltitudeKm() { return altitude * Constants.M_TO_KM; }
    public double getRadiusKm() { return radius * Constants.M_TO_KM; }
}
