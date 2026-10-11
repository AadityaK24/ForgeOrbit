public class LunarMissionResult
{
    private final double departureDeltaV;
    private final double lunarCaptureDeltaV;
    private final double totalDeltaV;
    private final double transferTimeSeconds;
    private final double lunarArrivalExcessVelocity;
    private final double targetLunarOrbitAltitudeKm;
    private final String assumptions;

    public LunarMissionResult(double departureDeltaV, double lunarCaptureDeltaV, double transferTimeSeconds,
        double lunarArrivalExcessVelocity, double targetLunarOrbitAltitudeKm, String assumptions)
    {
        this.departureDeltaV = requireNonNegative(departureDeltaV, "Departure delta-v");
        this.lunarCaptureDeltaV = requireNonNegative(lunarCaptureDeltaV, "Lunar capture delta-v");
        this.totalDeltaV = departureDeltaV + lunarCaptureDeltaV;
        this.transferTimeSeconds = requireNonNegative(transferTimeSeconds, "Transfer time");
        this.lunarArrivalExcessVelocity = requireNonNegative(lunarArrivalExcessVelocity, "Arrival excess velocity");
        this.targetLunarOrbitAltitudeKm = requireNonNegative(targetLunarOrbitAltitudeKm, "Lunar orbit altitude");
        this.assumptions = assumptions == null ? "" : assumptions;
    }

    public double getDepartureDeltaV() { return departureDeltaV; }
    public double getLunarCaptureDeltaV() { return lunarCaptureDeltaV; }
    public double getTotalDeltaV() { return totalDeltaV; }
    public double getTransferTimeSeconds() { return transferTimeSeconds; }
    public double getTransferTimeDays() { return transferTimeSeconds / Constants.SECONDS_PER_DAY; }
    public double getLunarArrivalExcessVelocity() { return lunarArrivalExcessVelocity; }
    public double getTargetLunarOrbitAltitudeKm() { return targetLunarOrbitAltitudeKm; }
    public String getAssumptions() { return assumptions; }

    private static double requireNonNegative(double value, String label)
    {
        if (!Double.isFinite(value) || value < 0.0) throw new IllegalArgumentException(label + " must be finite and non-negative.");
        return value;
    }
}
