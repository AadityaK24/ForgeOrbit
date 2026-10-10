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
        boolean feasible = required <= availablePropellant + 1e-9 && availableDeltaV + 1e-9 >= transfer.getTotalDeltaV();
        String message = feasible ? "Transfer is feasible with the supplied propellant." : "Insufficient propellant for the requested transfer.";
        return new MissionResult(transfer, required, availablePropellant, availableDeltaV, feasible, message);
    }

    public MissionResult analyse(Mission mission) { return analyze(mission); }
    public MissionPlanResult analyze(MissionRequest request) { return new MissionPlanner().analyze(request); }
    public MissionPlanResult analyse(MissionRequest request) { return analyze(request); }
}
