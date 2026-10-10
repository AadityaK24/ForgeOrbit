public class TransferResult
{
    private final double transferSemiMajorAxis;
    private final double firstBurnDeltaV;
    private final double secondBurnDeltaV;
    private final double totalDeltaV;
    private final double transferTime;

    public TransferResult(double transferSemiMajorAxis, double firstBurnDeltaV,
        double secondBurnDeltaV, double totalDeltaV, double transferTime)
    {
        requirePositive(transferSemiMajorAxis, "Transfer semi-major axis");
        requireNonNegative(firstBurnDeltaV, "First burn delta-v");
        requireNonNegative(secondBurnDeltaV, "Second burn delta-v");
        requireNonNegative(totalDeltaV, "Total delta-v");
        requireNonNegative(transferTime, "Transfer time");
        this.transferSemiMajorAxis = transferSemiMajorAxis;
        this.firstBurnDeltaV = firstBurnDeltaV;
        this.secondBurnDeltaV = secondBurnDeltaV;
        this.totalDeltaV = totalDeltaV;
        this.transferTime = transferTime;
    }

    public double getTransferSemiMajorAxis() { return transferSemiMajorAxis; }
    public double getFirstBurnDeltaV() { return firstBurnDeltaV; }
    public double getSecondBurnDeltaV() { return secondBurnDeltaV; }
    public double getTotalDeltaV() { return totalDeltaV; }
    public double getTransferTime() { return transferTime; }
    public double getTransferTimeSeconds() { return transferTime; }

    private static void requirePositive(double value, String label)
    {
        if (!Double.isFinite(value) || value <= 0.0) throw new IllegalArgumentException(label + " must be finite and positive.");
    }
    private static void requireNonNegative(double value, String label)
    {
        if (!Double.isFinite(value) || value < 0.0) throw new IllegalArgumentException(label + " must be finite and non-negative.");
    }
}
