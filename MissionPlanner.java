import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Mission planner compatible with both the original dashboard API and the V5 request API. */
public class MissionPlanner
{
    private final PropellantCalculator propellantCalculator = new PropellantCalculator();
    private final HohmannTransfer earthTransfer = new HohmannTransfer();
    private final LunarMissionCalculator lunarCalculator = new LunarMissionCalculator();
    private final InterplanetaryTransfer interplanetaryCalculator = new InterplanetaryTransfer();

    public MissionPlanResult createPlan(MissionRequest request)
    {
        return analyze(request);
    }

    public MissionPlanResult analyse(MissionRequest request)
    {
        return analyze(request);
    }

    public MissionPlanResult analyze(MissionRequest request)
    {
        if (request == null) throw new IllegalArgumentException("Mission request cannot be null.");

        // The compact V5 constructor intentionally requests a trajectory-only estimate.
        if (request.isSimplifiedRequest())
        {
            double deltaV;
            double transferTime;
            String summary;
            if (request.getMissionType() == MissionType.EARTH_ORBIT || request.getMissionType() == MissionType.SATELLITE)
            {
                double startAltitude = Math.max(1.0, request.getLaunchAltitudeMetres());
                double targetAltitude = Math.max(1.0, request.getTargetAltitudeMetres());
                TransferResult transfer = earthTransfer.calculateTransfer(new Orbit(startAltitude), new Orbit(targetAltitude));
                deltaV = transfer.getTotalDeltaV();
                transferTime = transfer.getTransferTime();
                summary = "Ideal Hohmann transfer between circular Earth orbits; propulsion feasibility was not evaluated.";
            }
            else if (request.getMissionType() == MissionType.LUNAR)
            {
                LunarMissionResult result = lunarCalculator.calculateTransfer(
                    Math.max(100000.0, request.getLaunchAltitudeMetres()));
                deltaV = result.getTotalDeltaV();
                transferTime = result.getTransferTimeSeconds();
                summary = "Simplified Earth-to-lunar-orbit estimate; propulsion feasibility was not evaluated.";
            }
            else
            {
                InterplanetaryTransferResult result = interplanetaryCalculator.calculateTransfer(
                    request.getOriginBodyName(), request.getDestinationBodyName());
                deltaV = result.getTotalDeltaV();
                transferTime = result.getTransferTimeSeconds();
                summary = "Ideal coplanar Hohmann estimate; launch windows and escape/capture burns are not modelled.";
            }
            return new MissionPlanResult(request.getMissionName(), request.getMissionType(), summary,
                deltaV, transferTime, true);
        }

        double deltaV;
        double transferSeconds;
        double launchWaitSeconds = 0.0;
        String origin;
        String destination;
        LocalDateTime departure = request.getLaunchDateTime();
        List<String> assumptions = new ArrayList<String>();

        if (request.getMissionType() == MissionType.SATELLITE || request.getMissionType() == MissionType.EARTH_ORBIT)
        {
            Orbit start = new Orbit(Math.max(1.0, request.getInitialEarthOrbitAltitudeKm() * Constants.KM_TO_M));
            Orbit target = new Orbit(Math.max(1.0, request.getTargetEarthOrbitAltitudeKm() * Constants.KM_TO_M));
            TransferResult result = earthTransfer.calculateTransfer(start, target);
            deltaV = result.getTotalDeltaV();
            transferSeconds = result.getTransferTime();
            origin = String.format("Earth orbit %.0f km", request.getInitialEarthOrbitAltitudeKm());
            destination = String.format("Earth orbit %.0f km", request.getTargetEarthOrbitAltitudeKm());
            assumptions.add("Two-body Earth model with impulsive Hohmann burns between circular, coplanar orbits.");
        }
        else if (request.getMissionType() == MissionType.LUNAR)
        {
            LunarMissionResult result = lunarCalculator.calculateTransfer(
                Math.max(100.0, request.getInitialEarthOrbitAltitudeKm()), request.getTargetLunarOrbitAltitudeKm());
            deltaV = result.getTotalDeltaV();
            transferSeconds = result.getTransferTimeSeconds();
            origin = String.format("Earth orbit %.0f km", request.getInitialEarthOrbitAltitudeKm());
            destination = String.format("Lunar orbit %.0f km", request.getTargetLunarOrbitAltitudeKm());
            assumptions.add(result.getAssumptions());
        }
        else
        {
            InterplanetaryTransferResult result = interplanetaryCalculator.calculateTransfer(
                request.getOriginPlanet(), request.getDestinationPlanet(), request.getLaunchDateTime());
            deltaV = result.getTotalDeltaV();
            transferSeconds = result.getTransferTimeSeconds();
            launchWaitSeconds = result.getLaunchWaitSeconds();
            departure = result.getEstimatedDeparture();
            origin = result.getOrigin();
            destination = result.getDestination();
            assumptions.add(result.getAssumptions());
        }

        Propulsion propulsion = new Propulsion("Mission Engine", request.getSpecificImpulseSeconds(),
            request.getThrustNewtons(), request.getPropulsionEfficiency());
        double requiredPropellant = propellantCalculator.calculateRequiredPropellant(
            deltaV, request.getDryMassKg(), propulsion);
        double availablePropellant = request.getPropellantMassKg();
        double availableDeltaV = propellantCalculator.calculateAvailableDeltaV(
            request.getDryMassKg(), availablePropellant, propulsion);
        boolean feasible = requiredPropellant <= availablePropellant + 1e-9
            && availableDeltaV + 1e-9 >= deltaV;
        String status = feasible ? "FEASIBLE UNDER MODEL ASSUMPTIONS"
            : "INSUFFICIENT PROPELLANT UNDER MODEL ASSUMPTIONS";
        assumptions.add("Propellant estimate uses the ideal rocket equation and treats total delta-v as a single equivalent manoeuvre budget.");
        assumptions.add("This planning result is an educational first-order estimate, not a flight-ready trajectory or mission design.");
        return new MissionPlanResult(request.getMissionName(), request.getMissionType(), origin, destination,
            deltaV, requiredPropellant, availablePropellant, availableDeltaV, transferSeconds,
            launchWaitSeconds, feasible, status, assumptions, departure);
    }
}
