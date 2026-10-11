import java.time.LocalDateTime;

public class InterplanetaryTransferResult
{
    private final String origin;
    private final String destination;
    private final LocalDateTime requestedDeparture;
    private final LocalDateTime estimatedDeparture;
    private final double launchWaitSeconds;
    private final double transferTimeSeconds;
    private final double departureDeltaV;
    private final double arrivalDeltaV;
    private final double totalDeltaV;
    private final double requiredPhaseAngleRadians;
    private final double actualPhaseAngleRadians;
    private final String assumptions;
    private double transferSemiMajorAxisMetres;

    public InterplanetaryTransferResult(String originBodyName, String destinationBodyName,
        double transferSemiMajorAxisMetres, double departureDeltaV, double arrivalDeltaV,
        double totalDeltaV, double transferTimeSeconds)
    {
        this(originBodyName, destinationBodyName, LocalDateTime.of(2000, 1, 1, 12, 0),
            LocalDateTime.of(2000, 1, 1, 12, 0), 0.0, transferTimeSeconds,
            departureDeltaV, arrivalDeltaV, 0.0, 0.0,
            "Ideal coplanar Hohmann estimate; launch windows and planetary escape/capture are not modelled.");
        this.transferSemiMajorAxisMetres = transferSemiMajorAxisMetres;
    }

    public InterplanetaryTransferResult(String origin, String destination, LocalDateTime requestedDeparture,
        LocalDateTime estimatedDeparture, double launchWaitSeconds, double transferTimeSeconds,
        double departureDeltaV, double arrivalDeltaV, double requiredPhaseAngleRadians,
        double actualPhaseAngleRadians, String assumptions)
    {
        if (origin == null || destination == null || requestedDeparture == null || estimatedDeparture == null) throw new IllegalArgumentException("Transfer names and dates cannot be null.");
        this.origin = origin;
        this.destination = destination;
        this.requestedDeparture = requestedDeparture;
        this.estimatedDeparture = estimatedDeparture;
        this.launchWaitSeconds = nonNegative(launchWaitSeconds, "Launch wait");
        this.transferTimeSeconds = nonNegative(transferTimeSeconds, "Transfer time");
        this.departureDeltaV = nonNegative(departureDeltaV, "Departure delta-v");
        this.arrivalDeltaV = nonNegative(arrivalDeltaV, "Arrival delta-v");
        this.totalDeltaV = departureDeltaV + arrivalDeltaV;
        this.requiredPhaseAngleRadians = requiredPhaseAngleRadians;
        this.actualPhaseAngleRadians = actualPhaseAngleRadians;
        this.assumptions = assumptions == null ? "" : assumptions;
        this.transferSemiMajorAxisMetres = transferTimeSeconds > 0.0
            ? Math.cbrt(Constants.SUN_MU * Math.pow(transferTimeSeconds / Math.PI, 2.0)) : 0.0;
    }

    public String getOrigin() { return origin; }
    public String getOriginBodyName() { return origin; }
    public String getDestination() { return destination; }
    public String getDestinationBodyName() { return destination; }
    public LocalDateTime getRequestedDeparture() { return requestedDeparture; }
    public LocalDateTime getEstimatedDeparture() { return estimatedDeparture; }
    public double getLaunchWaitSeconds() { return launchWaitSeconds; }
    public double getLaunchWaitDays() { return launchWaitSeconds / Constants.SECONDS_PER_DAY; }
    public double getTransferTimeSeconds() { return transferTimeSeconds; }
    public double getTransferTimeDays() { return transferTimeSeconds / Constants.SECONDS_PER_DAY; }
    public double getDepartureDeltaV() { return departureDeltaV; }
    public double getTransferSemiMajorAxisMetres() { return transferSemiMajorAxisMetres; }
    public double getArrivalDeltaV() { return arrivalDeltaV; }
    public double getTotalDeltaV() { return totalDeltaV; }
    public double getRequiredPhaseAngleRadians() { return requiredPhaseAngleRadians; }
    public double getRequiredPhaseAngleDegrees() { return requiredPhaseAngleRadians * Constants.RAD_TO_DEG; }
    public double getActualPhaseAngleRadians() { return actualPhaseAngleRadians; }
    public double getActualPhaseAngleDegrees() { return actualPhaseAngleRadians * Constants.RAD_TO_DEG; }
    public String getAssumptions() { return assumptions; }

    private static double nonNegative(double value, String label)
    {
        if (!Double.isFinite(value) || value < 0.0) throw new IllegalArgumentException(label + " must be finite and non-negative.");
        return value;
    }
}
