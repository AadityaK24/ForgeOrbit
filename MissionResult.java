public class MissionResult
{
    private final TransferResult transferResult;
    private final double requiredPropellant;
    private final double availableDeltaV;
    private final double deltaVMargin;
    private final double deltaVMarginPercentage;
    private final boolean missionFeasible;
    private final String verdict;

    public MissionResult(
        TransferResult transferResult,
        double requiredPropellant,
        double availableDeltaV,
        double deltaVMargin,
        double deltaVMarginPercentage,
        boolean missionFeasible,
        String verdict)
    {
        if (transferResult == null)
        {
            throw new IllegalArgumentException(
                "Transfer result cannot be null."
            );
        }

        if (requiredPropellant < 0)
        {
            throw new IllegalArgumentException(
                "Required propellant cannot be negative."
            );
        }

        if (availableDeltaV < 0)
        {
            throw new IllegalArgumentException(
                "Available delta-v cannot be negative."
            );
        }

        if (verdict == null || verdict.trim().isEmpty())
        {
            throw new IllegalArgumentException(
                "Verdict cannot be empty."
            );
        }

        this.transferResult = transferResult;
        this.requiredPropellant = requiredPropellant;
        this.availableDeltaV = availableDeltaV;
        this.deltaVMargin = deltaVMargin;
        this.deltaVMarginPercentage = deltaVMarginPercentage;
        this.missionFeasible = missionFeasible;
        this.verdict = verdict;
    }

    public TransferResult getTransferResult()
    {
        return transferResult;
    }

    public double getRequiredPropellant()
    {
        return requiredPropellant;
    }

    public double getAvailableDeltaV()
    {
        return availableDeltaV;
    }

    public double getDeltaVMargin()
    {
        return deltaVMargin;
    }

    public double getDeltaVMarginPercentage()
    {
        return deltaVMarginPercentage;
    }

    public boolean isMissionFeasible()
    {
        return missionFeasible;
    }

    public String getVerdict()
    {
        return verdict;
    }
}