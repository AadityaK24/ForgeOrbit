public class Mission
{
    private final String missionName;
    private final Satellite satellite;
    private final Propulsion propulsion;
    private final HohmannTransfer hohmannTransfer;

    public Mission(
        String missionName,
        Satellite satellite,
        Propulsion propulsion)
    {
        if (missionName == null || missionName.trim().isEmpty())
        {
            throw new IllegalArgumentException(
                "Mission name cannot be empty."
            );
        }

        if (satellite == null)
        {
            throw new IllegalArgumentException(
                "Satellite cannot be null."
            );
        }

        if (propulsion == null)
        {
            throw new IllegalArgumentException(
                "Propulsion system cannot be null."
            );
        }

        this.missionName = missionName;
        this.satellite = satellite;
        this.propulsion = propulsion;
        hohmannTransfer = new HohmannTransfer();
    }

    public String getMissionName()
    {
        return missionName;
    }

    public Satellite getSatellite()
    {
        return satellite;
    }

    public Propulsion getPropulsion()
    {
        return propulsion;
    }

    public Orbit getInitialOrbit()
    {
        return satellite.getCurrentOrbit();
    }

    public Orbit getTargetOrbit()
    {
        return satellite.getTargetOrbit();
    }

    public TransferResult calculateTransfer()
    {
        Orbit initialOrbit = getInitialOrbit();
        Orbit targetOrbit = getTargetOrbit();

        double transferSemiMajorAxis =
            hohmannTransfer.calculateTransferSemiMajorAxis(
                initialOrbit,
                targetOrbit
            );

        double firstBurnDeltaV =
            hohmannTransfer.calculateFirstBurnDeltaV(
                initialOrbit,
                targetOrbit
            );

        double secondBurnDeltaV =
            hohmannTransfer.calculateSecondBurnDeltaV(
                initialOrbit,
                targetOrbit
            );

        double totalDeltaV =
            hohmannTransfer.calculateTotalDeltaV(
                initialOrbit,
                targetOrbit
            );

        double transferTime =
            hohmannTransfer.calculateTransferTime(
                initialOrbit,
                targetOrbit
            );

        return new TransferResult(
            transferSemiMajorAxis,
            firstBurnDeltaV,
            secondBurnDeltaV,
            totalDeltaV,
            transferTime
        );
    }
}