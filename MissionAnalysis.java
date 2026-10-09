
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
        validateMissionAndTransfer(mission, transferResult);

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
        validateMissionAndTransfer(mission, transferResult);

        return calculateAvailableDeltaV(mission)
            - transferResult.getTotalDeltaV();
    }

    public double calculateDeltaVMarginPercentage(
        Mission mission,
        TransferResult transferResult)
    {
        validateMissionAndTransfer(mission, transferResult);

        double requiredDeltaV =
            transferResult.getTotalDeltaV();

        if (requiredDeltaV == 0.0)
        {
            return 0.0;
        }

        return calculateDeltaVMargin(mission, transferResult)
            / requiredDeltaV * 100.0;
    }

    public boolean isMissionFeasible(
        Mission mission,
        TransferResult transferResult)
    {
        return analyzeMission(mission, transferResult)
            .isMissionFeasible();
    }

    public String generateVerdict(
        Mission mission,
        TransferResult transferResult)
    {
        return analyzeMission(mission, transferResult)
            .getVerdict();
    }

    public MissionResult analyzeMission(
        Mission mission,
        TransferResult transferResult)
    {
        validateMissionAndTransfer(mission, transferResult);

        double requiredDeltaV =
            transferResult.getTotalDeltaV();

        double requiredPropellant =
            calculateRequiredPropellant(mission, transferResult);

        double availableDeltaV =
            calculateAvailableDeltaV(mission);

        double deltaVMargin =
            availableDeltaV - requiredDeltaV;

        double marginPercentage =
            requiredDeltaV > 0.0
                ? deltaVMargin / requiredDeltaV * 100.0
                : 0.0;

        boolean feasible =
            availableDeltaV >= requiredDeltaV;

        String verdict;

        if (requiredDeltaV == 0.0)
        {
            verdict = "NO TRANSFER REQUIRED";
        }
        else if (!feasible)
        {
            verdict = "MISSION NOT FEASIBLE";
        }
        else if (marginPercentage < 10.0)
        {
            verdict = "LOW DELTA-V MARGIN";
        }
        else
        {
            verdict = "MISSION FEASIBLE";
        }

        return new MissionResult(
            transferResult,
            requiredPropellant,
            availableDeltaV,
            deltaVMargin,
            marginPercentage,
            feasible,
            verdict
        );
    }

    private void validateMissionAndTransfer(
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

        double deltaV = transferResult.getTotalDeltaV();

        if (!Double.isFinite(deltaV) || deltaV < 0.0)
        {
            throw new IllegalArgumentException(
                "Transfer delta-v must be finite and non-negative."
            );
        }
    }
}
