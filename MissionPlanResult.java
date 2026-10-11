import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MissionPlanResult
{
    private final String missionName;
    private final MissionType missionType;
    private final String origin;
    private final String destination;
    private final double totalDeltaV;
    private final double requiredPropellantKg;
    private final double availablePropellantKg;
    private final double availableDeltaV;
    private final double transferTimeSeconds;
    private final double launchWaitSeconds;
    private final boolean feasible;
    private final String status;
    private final List<String> assumptions;
    private final LocalDateTime estimatedDeparture;

    public MissionPlanResult(String missionName, MissionType missionType, String origin, String destination,
        double totalDeltaV, double requiredPropellantKg, double availablePropellantKg,
        double availableDeltaV, double transferTimeSeconds, double launchWaitSeconds,
        boolean feasible, String status, List<String> assumptions, LocalDateTime estimatedDeparture)
    {
        this.missionName = missionName == null ? "ForgeOrbit Mission" : missionName;
        this.missionType = missionType;
        this.origin = origin == null ? "" : origin;
        this.destination = destination == null ? "" : destination;
        this.totalDeltaV = totalDeltaV;
        this.requiredPropellantKg = requiredPropellantKg;
        this.availablePropellantKg = availablePropellantKg;
        this.availableDeltaV = availableDeltaV;
        this.transferTimeSeconds = transferTimeSeconds;
        this.launchWaitSeconds = launchWaitSeconds;
        this.feasible = feasible;
        this.status = status == null ? "" : status;
        this.assumptions = Collections.unmodifiableList(new ArrayList<String>(assumptions == null
            ? Collections.<String>emptyList() : assumptions));
        this.estimatedDeparture = estimatedDeparture;
    }

    // V5 compact-result constructor retained alongside the richer legacy result.
    public MissionPlanResult(String missionName, MissionType missionType, String summary,
                             double totalDeltaV, double transferTimeSeconds, boolean feasible)
    {
        this(missionName, missionType, "", "", totalDeltaV, 0.0, 0.0, 0.0,
            transferTimeSeconds, 0.0, feasible, summary, Collections.<String>emptyList(), null);
    }

    public String getMissionName() { return missionName; }
    public MissionType getMissionType() { return missionType; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public String getSummary() { return status; }
    public double getTotalDeltaV() { return totalDeltaV; }
    public double getRequiredPropellantKg() { return requiredPropellantKg; }
    public double getAvailablePropellantKg() { return availablePropellantKg; }
    public double getAvailableDeltaV() { return availableDeltaV; }
    public double getDeltaVMargin() { return availableDeltaV - totalDeltaV; }
    public double getTransferTimeSeconds() { return transferTimeSeconds; }
    public double getTransferTimeDays() { return transferTimeSeconds / Constants.SECONDS_PER_DAY; }
    public double getLaunchWaitSeconds() { return launchWaitSeconds; }
    public double getLaunchWaitDays() { return launchWaitSeconds / Constants.SECONDS_PER_DAY; }
    public boolean isFeasible() { return feasible; }
    public String getStatus() { return status; }
    public List<String> getAssumptions() { return assumptions; }
    public LocalDateTime getEstimatedDeparture() { return estimatedDeparture; }
}
