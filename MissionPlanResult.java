public final class MissionPlanResult
{
    private final String missionName;
    private final MissionType missionType;
    private final String summary;
    private final double totalDeltaV;
    private final double transferTimeSeconds;
    private final boolean feasible;

    public MissionPlanResult(String missionName, MissionType missionType, String summary,
                             double totalDeltaV, double transferTimeSeconds, boolean feasible)
    {
        this.missionName = missionName;
        this.missionType = missionType;
        this.summary = summary;
        this.totalDeltaV = totalDeltaV;
        this.transferTimeSeconds = transferTimeSeconds;
        this.feasible = feasible;
    }

    public String getMissionName() { return missionName; }
    public MissionType getMissionType() { return missionType; }
    public String getSummary() { return summary; }
    public double getTotalDeltaV() { return totalDeltaV; }
    public double getTransferTimeSeconds() { return transferTimeSeconds; }
    public double getTransferTimeDays() { return transferTimeSeconds / Constants.SECONDS_PER_DAY; }
    public boolean isFeasible() { return feasible; }
}
