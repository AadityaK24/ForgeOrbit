public class MissionAnalysis
{
    private final PropellantCalculator propellantCalculator;

    public MissionAnalysis() { this(new PropellantCalculator()); }

    public MissionAnalysis(PropellantCalculator calculator)
    {
        if (calculator == null) throw new IllegalArgumentException("Propellant calculator cannot be null.");
        propellantCalculator = calculator;
    }

    public MissionResult analyze(Mission mission)
    {
        if (mission == null) throw new IllegalArgumentException("Mission cannot be null.");
        TransferResult transfer = mission.calculateTransfer();
        Satellite satellite = mission.getSatellite();
        Propulsion propulsion = mission.getPropulsion();
        if (satellite == null || propulsion == null)
        {
            return new MissionResult(transfer, 0.0, 0.0, 0.0, true,
                "Orbital transfer calculated. Spacecraft mass and propulsion were not supplied, so fuel feasibility was not evaluated.");
        }
        double required = propellantCalculator.calculateRequiredPropellant(
            transfer.getTotalDeltaV(), satellite.getDryMass(), propulsion);
        double availablePropellant = satellite.getPropellantMass();
        double availableDeltaV = propellantCalculator.calculateAvailableDeltaV(
            satellite.getDryMass(), availablePropellant, propulsion);
        boolean feasible = required <= availablePropellant + 1e-9
            && availableDeltaV + 1e-9 >= transfer.getTotalDeltaV();
        String message = feasible ? "Transfer is feasible with the supplied propellant."
            : "Insufficient propellant for the requested transfer.";
        return new MissionResult(transfer, required, availablePropellant, availableDeltaV, feasible, message);
    }

    public MissionResult analyse(Mission mission) { return analyze(mission); }

    public MissionPlanResult analyze(MissionRequest request)
    {
        return new MissionPlanner().analyze(request);
    }

    public MissionPlanResult analyse(MissionRequest request) { return analyze(request); }

    public double calculateRequiredPropellant(Mission mission, TransferResult result)
    {
        if (mission == null || result == null) throw new IllegalArgumentException("Mission and transfer result are required.");
        Satellite satellite = mission.getSatellite();
        Propulsion propulsion = mission.getPropulsion();
        if (satellite == null || propulsion == null) return 0.0;
        return propellantCalculator.calculateRequiredPropellant(result.getTotalDeltaV(), satellite.getDryMass(), propulsion);
    }

    public double calculateAvailableDeltaV(Mission mission)
    {
        if (mission == null) throw new IllegalArgumentException("Mission cannot be null.");
        Satellite satellite = mission.getSatellite();
        Propulsion propulsion = mission.getPropulsion();
        if (satellite == null || propulsion == null) return 0.0;
        return propellantCalculator.calculateAvailableDeltaV(
            satellite.getDryMass(), satellite.getPropellantMass(), propulsion);
    }

    public double calculateDeltaVMargin(Mission mission, TransferResult result)
    {
        if (result == null) throw new IllegalArgumentException("Transfer result cannot be null.");
        return calculateAvailableDeltaV(mission) - result.getTotalDeltaV();
    }

    public boolean isMissionFeasible(Mission mission, TransferResult result)
    {
        if (result == null) throw new IllegalArgumentException("Transfer result cannot be null.");
        Satellite satellite = mission == null ? null : mission.getSatellite();
        Propulsion propulsion = mission == null ? null : mission.getPropulsion();
        if (satellite == null || propulsion == null) return true;
        return calculateRequiredPropellant(mission, result) <= satellite.getPropellantMass() + 1.0e-9
            && calculateAvailableDeltaV(mission) + 1.0e-9 >= result.getTotalDeltaV();
    }

    public String generateVerdict(Mission mission, TransferResult result)
    {
        if (result == null) throw new IllegalArgumentException("Transfer result cannot be null.");
        if (mission == null || mission.getSatellite() == null || mission.getPropulsion() == null)
            return "TRANSFER CALCULATED; PROPULSION FEASIBILITY NOT EVALUATED";
        return isMissionFeasible(mission, result)
            ? "MISSION FEASIBLE UNDER MODEL ASSUMPTIONS"
            : "MISSION NOT FEASIBLE WITH AVAILABLE PROPELLANT";
    }
}
