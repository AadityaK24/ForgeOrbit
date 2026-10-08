public class MissionAnalysis
{
    private final PropellantCalculator propellantCalculator;

    public MissionAnalysis()
    {
        propellantCalculator = new PropellantCalculator();
    }

    public double calculateRequiredPropellant(
        Mission mission,
        TransferResult transferResult)
    {
        if (mission == null)
        {
            throw new IllegalArgumentException(
                "Mission cannot be null."
            );
        }

        if (transferResult == null)
        {
            throw new IllegalArgumentException(
                "Transfer result cannot be null."
            );
        }

        Satellite satellite = mission.getSatellite();

        return propellantCalculator.calculateRequiredPropellant(
            transferResult.getTotalDeltaV(),
            satellite.getDryMass(),
            mission.getPropulsion()
        );
    }

    public double calculateAvailableDeltaV(Mission mission)
    {
        if (mission == null)
        {
            throw new IllegalArgumentException(
                "Mission cannot be null."
            );
        }

        Satellite satellite = mission.getSatellite();

        return propellantCalculator.calculateAvailableDeltaV(
            satellite.getDryMass(),
            satellite.getPropellantMass(),
            mission.getPropulsion()
        );
    }

    public double calculateDeltaVMargin(
        Mission mission,
        TransferResult transferResult)
    {
        double requiredDeltaV =
            transferResult.getTotalDeltaV();

        double availableDeltaV =
            calculateAvailableDeltaV(mission);

        return availableDeltaV - requiredDeltaV;
    }

    public double calculateDeltaVMarginPercentage(
        Mission mission,
        TransferResult transferResult)
    {
        double requiredDeltaV =
            transferResult.getTotalDeltaV();

        if (requiredDeltaV <= 0)
        {
            throw new IllegalArgumentException(
                "Required delta-v must be greater than 0."
            );
        }

        double margin =
            calculateDeltaVMargin(mission, transferResult);

        return (margin / requiredDeltaV) * 100.0;
    }

    public boolean isMissionFeasible(
        Mission mission,
        TransferResult transferResult)
    {
        return calculateAvailableDeltaV(mission)
            >= transferResult.getTotalDeltaV();
    }

    public String generateVerdict(
        Mission mission,
        TransferResult transferResult)
    {
        double requiredDeltaV =
            transferResult.getTotalDeltaV();

        double availableDeltaV =
            calculateAvailableDeltaV(mission);

        double marginPercentage =
            calculateDeltaVMarginPercentage(
                mission,
                transferResult
            );

        if (availableDeltaV < requiredDeltaV)
        {
            return "MISSION NOT FEASIBLE";
        }

        if (marginPercentage < 10.0)
        {
            return "LOW DELTA-V MARGIN";
        }

        return "MISSION FEASIBLE";
    }
}