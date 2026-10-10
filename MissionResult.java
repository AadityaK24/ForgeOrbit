public class MissionResult
{
    private final TransferResult transferResult;
    private final double requiredPropellant;
    private final double availablePropellant;
    private final double availableDeltaV;
    private final double deltaVMargin;
    private final boolean feasible;
    private final String message;

    public MissionResult(TransferResult transferResult, double requiredPropellant,
        double availablePropellant, double availableDeltaV, boolean feasible, String message)
    {
        if (transferResult == null) throw new IllegalArgumentException("Transfer result cannot be null.");
        requireNonNegative(requiredPropellant, "Required propellant");
        requireNonNegative(availablePropellant, "Available propellant");
        requireNonNegative(availableDeltaV, "Available delta-v");
        this.transferResult = transferResult;
        this.requiredPropellant = requiredPropellant;
        this.availablePropellant = availablePropellant;
        this.availableDeltaV = availableDeltaV;
        this.deltaVMargin = availableDeltaV - transferResult.getTotalDeltaV();
        this.feasible = feasible;
        this.message = message == null ? "" : message;
    }

    public TransferResult getTransferResult() { return transferResult; }
    public double getTotalDeltaV() { return transferResult.getTotalDeltaV(); }
    public double getFirstBurnDeltaV() { return transferResult.getFirstBurnDeltaV(); }
    public double getSecondBurnDeltaV() { return transferResult.getSecondBurnDeltaV(); }
    public double getTransferTime() { return transferResult.getTransferTime(); }
    public double getRequiredPropellant() { return requiredPropellant; }
    public double getRequiredPropellantMass() { return requiredPropellant; }
    public double getAvailablePropellant() { return availablePropellant; }
    public double getAvailablePropellantMass() { return availablePropellant; }
    public double getAvailableDeltaV() { return availableDeltaV; }
    public double getDeltaVMargin() { return deltaVMargin; }
    public boolean isFeasible() { return feasible; }
    public String getMessage() { return message; }
    public String getStatusMessage() { return message; }

    private static void requireNonNegative(double value, String label)
    {
        if (!Double.isFinite(value) || value < 0.0) throw new IllegalArgumentException(label + " must be finite and non-negative.");
    }
}
