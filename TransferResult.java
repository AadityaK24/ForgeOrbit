public class TransferResult
{
    private final double transferSemiMajorAxis;
    private final double firstBurnDeltaV;
    private final double secondBurnDeltaV;
    private final double totalDeltaV;
    private final double transferTime;

    public TransferResult(
        double transferSemiMajorAxis,
        double firstBurnDeltaV,
        double secondBurnDeltaV,
        double totalDeltaV,
        double transferTime)
    {
        this.transferSemiMajorAxis = transferSemiMajorAxis;
        this.firstBurnDeltaV = firstBurnDeltaV;
        this.secondBurnDeltaV = secondBurnDeltaV;
        this.totalDeltaV = totalDeltaV;
        this.transferTime = transferTime;
    }

    public double getTransferSemiMajorAxis()
    {
        return transferSemiMajorAxis;
    }

    public double getFirstBurnDeltaV()
    {
        return firstBurnDeltaV;
    }

    public double getSecondBurnDeltaV()
    {
        return secondBurnDeltaV;
    }

    public double getTotalDeltaV()
    {
        return totalDeltaV;
    }

    public double getTransferTime()
    {
        return transferTime;
    }
}