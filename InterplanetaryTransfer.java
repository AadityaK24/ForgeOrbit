import java.time.Duration;
import java.time.LocalDateTime;

/** Ideal coplanar heliocentric Hohmann estimate with a low-fidelity launch-phase estimate. */
public class InterplanetaryTransfer
{
    public InterplanetaryTransferResult calculateTransfer(String originName, String destinationName)
    {
        return calculateTransfer(originName, destinationName, LocalDateTime.of(2000, 1, 1, 12, 0));
    }

    public InterplanetaryTransferResult calculateTransfer(CelestialBody origin, CelestialBody destination)
    {
        if (origin == null || destination == null) throw new IllegalArgumentException("Origin and destination cannot be null.");
        return calculateTransfer(origin.getName(), destination.getName());
    }

    public InterplanetaryTransferResult calculateTransfer(String originName, String destinationName,
                                                            LocalDateTime requestedDeparture)
    {
        if (requestedDeparture == null) throw new IllegalArgumentException("Departure date cannot be null.");
        CelestialBody origin = SolarSystemCatalog.getBody(originName);
        CelestialBody destination = SolarSystemCatalog.getBody(destinationName);
        if (origin.isStar() || destination.isStar()) throw new IllegalArgumentException("Choose two planets, not the Sun.");
        if (origin.getName().equalsIgnoreCase(destination.getName())) throw new IllegalArgumentException("Origin and destination must differ.");

        double r1 = origin.getOrbitalElements().getSemiMajorAxisAu() * Constants.AU_TO_M;
        double r2 = destination.getOrbitalElements().getSemiMajorAxisAu() * Constants.AU_TO_M;
        double a = (r1 + r2) / 2.0;
        double time = Math.PI * Math.sqrt(a * a * a / Constants.SUN_MU);
        double v1 = Math.sqrt(Constants.SUN_MU / r1);
        double v2 = Math.sqrt(Constants.SUN_MU / r2);
        double vt1 = Math.sqrt(Constants.SUN_MU * (2.0 / r1 - 1.0 / a));
        double vt2 = Math.sqrt(Constants.SUN_MU * (2.0 / r2 - 1.0 / a));
        double departureDeltaV = Math.abs(vt1 - v1);
        double arrivalDeltaV = Math.abs(v2 - vt2);

        double n1 = Math.sqrt(Constants.SUN_MU / (r1 * r1 * r1));
        double n2 = Math.sqrt(Constants.SUN_MU / (r2 * r2 * r2));
        double requiredPhase = PlanetaryEphemeris.normalizeAngle(Math.PI - n2 * time);
        double currentPhase = PlanetaryEphemeris.getPhaseAngleRadians(originName, destinationName, requestedDeparture);
        double relativeRate = n2 - n1;
        double wait;
        if (Math.abs(relativeRate) < 1.0e-15) wait = 0.0;
        else if (relativeRate > 0.0) wait = PlanetaryEphemeris.normalizeAngle(requiredPhase - currentPhase) / relativeRate;
        else wait = PlanetaryEphemeris.normalizeAngle(currentPhase - requiredPhase) / (-relativeRate);
        // Avoid overflow for dates far outside ordinary mission-planning ranges.
        wait = Math.min(wait, 100.0 * Constants.SECONDS_PER_JULIAN_YEAR);
        LocalDateTime departure = requestedDeparture.plusNanos((long)(wait * 1.0e9));
        double actualPhase = PlanetaryEphemeris.getPhaseAngleRadians(originName, destinationName, departure);
        String assumptions = "Ideal coplanar circular heliocentric Hohmann estimate. Planetary phase uses an approximate Keplerian ephemeris; escape/capture burns, inclination changes, perturbations and finite burns are excluded.";
        return new InterplanetaryTransferResult(origin.getName(), destination.getName(), requestedDeparture,
            departure, wait, time, departureDeltaV, arrivalDeltaV, requiredPhase, actualPhase, assumptions);
    }

    public InterplanetaryTransferResult calculateTransfer(String originName, String destinationName,
                                                           LocalDateTime departure, boolean launchImmediately)
    {
        if (!launchImmediately) return calculateTransfer(originName, destinationName, departure);
        InterplanetaryTransferResult r = calculateTransfer(originName, destinationName, departure);
        return new InterplanetaryTransferResult(r.getOrigin(), r.getDestination(), departure, departure,
            0.0, r.getTransferTimeSeconds(), r.getDepartureDeltaV(), r.getArrivalDeltaV(),
            r.getRequiredPhaseAngleRadians(), PlanetaryEphemeris.getPhaseAngleRadians(originName, destinationName, departure),
            r.getAssumptions() + " Immediate-departure mode does not enforce ideal phase alignment.");
    }
}
