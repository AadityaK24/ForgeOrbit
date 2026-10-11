import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Idealised patched-conic estimate: Earth departure, coast to lunar distance, lunar capture.
public class LunarMissionCalculator
{
    // V5 overload: accepts initial Earth parking-orbit altitude in metres.
    public LunarMissionResult calculateTransfer(double initialEarthOrbitAltitudeMetres)
    {
        if (!Double.isFinite(initialEarthOrbitAltitudeMetres) || initialEarthOrbitAltitudeMetres < 100000.0)
            throw new IllegalArgumentException("Initial Earth orbit altitude must be at least 100 km.");
        return calculateTransfer(initialEarthOrbitAltitudeMetres / Constants.KM_TO_M, 100.0);
    }

    public LunarMissionResult calculateTransfer(double initialEarthOrbitAltitudeKm, double targetLunarOrbitAltitudeKm)
    {
        if (!Double.isFinite(initialEarthOrbitAltitudeKm) || initialEarthOrbitAltitudeKm < 100.0) throw new IllegalArgumentException("Initial Earth orbit altitude must be at least 100 km for this estimate.");
        if (!Double.isFinite(targetLunarOrbitAltitudeKm) || targetLunarOrbitAltitudeKm < 0.0) throw new IllegalArgumentException("Lunar orbit altitude must be finite and non-negative.");

        double muEarth = Constants.EARTH_MU;
        double r1 = Constants.EARTH_RADIUS + initialEarthOrbitAltitudeKm * Constants.KM_TO_M;
        double rMoon = Constants.EARTH_MOON_MEAN_DISTANCE;
        double a = (r1 + rMoon) / 2.0;
        double circularEarthVelocity = Math.sqrt(muEarth / r1);
        double transferPerigeeVelocity = Math.sqrt(muEarth * (2.0 / r1 - 1.0 / a));
        double departureDeltaV = Math.abs(transferPerigeeVelocity - circularEarthVelocity);
        double transferApoapsisVelocity = Math.sqrt(muEarth * (2.0 / rMoon - 1.0 / a));
        double moonOrbitalVelocity = Math.sqrt(muEarth / rMoon);
        double vInfinity = Math.abs(moonOrbitalVelocity - transferApoapsisVelocity);

        double lunarOrbitRadius = Constants.MOON_RADIUS + targetLunarOrbitAltitudeKm * Constants.KM_TO_M;
        double arrivalHyperbolicVelocity = Math.sqrt(vInfinity * vInfinity + 2.0 * Constants.MOON_MU / lunarOrbitRadius);
        double desiredLunarCircularVelocity = Math.sqrt(Constants.MOON_MU / lunarOrbitRadius);
        double captureDeltaV = Math.abs(arrivalHyperbolicVelocity - desiredLunarCircularVelocity);
        double transferTime = Math.PI * Math.sqrt(Math.pow(a, 3.0) / muEarth);
        String assumptions = "Idealised patched-conic estimate using a circular Earth orbit, mean Earth-Moon distance and impulsive manoeuvres. Launch-window geometry, lunar orbital eccentricity/inclination, finite burns, perturbations and landing/descent are not modelled.";
        return new LunarMissionResult(departureDeltaV, captureDeltaV, transferTime, vInfinity, targetLunarOrbitAltitudeKm, assumptions);
    }

    // Approximate illumination phase: reference new moon on 2000-01-06.
    // Output is 0..1, where 0=new, 0.5=full, and values near 1=new again.
    public double calculateApproximateMoonPhase(LocalDate date)
    {
        if (date == null) throw new IllegalArgumentException("Date cannot be null.");
        LocalDate referenceNewMoon = LocalDate.of(2000, 1, 6);
        long days = ChronoUnit.DAYS.between(referenceNewMoon, date);
        double phase = (days % Constants.MOON_SYNODIC_PERIOD_DAYS) / Constants.MOON_SYNODIC_PERIOD_DAYS;
        if (phase < 0.0) phase += 1.0;
        return phase;
    }

    public String getApproximateMoonPhaseName(double phaseFraction)
    {
        if (!Double.isFinite(phaseFraction)) throw new IllegalArgumentException("Phase must be finite.");
        double phase = phaseFraction - Math.floor(phaseFraction);
        if (phase < 0.0625 || phase >= 0.9375) return "New Moon";
        if (phase < 0.1875) return "Waxing Crescent";
        if (phase < 0.3125) return "First Quarter";
        if (phase < 0.4375) return "Waxing Gibbous";
        if (phase < 0.5625) return "Full Moon";
        if (phase < 0.6875) return "Waning Gibbous";
        if (phase < 0.8125) return "Last Quarter";
        return "Waning Crescent";
    }
}
