public class Mission
{
    private final String name;
    private final Orbit initialOrbit;
    private final Orbit targetOrbit;
    private final Satellite satellite;
    private final Propulsion propulsion;
    private final HohmannTransfer transferCalculator;

    public Mission(Orbit initialOrbit, Orbit targetOrbit)
    {
        this("Orbital Transfer", initialOrbit, targetOrbit, null, null, new HohmannTransfer());
    }

    public Mission(Satellite satellite, Propulsion propulsion)
    {
        this(satellite == null ? "Satellite Mission" : satellite.getName(),
            satellite == null ? null : satellite.getCurrentOrbit(),
            satellite == null ? null : satellite.getTargetOrbit(), satellite, propulsion, new HohmannTransfer());
    }

    public Mission(String name, Satellite satellite, Propulsion propulsion)
    {
        this(name, satellite == null ? null : satellite.getCurrentOrbit(),
            satellite == null ? null : satellite.getTargetOrbit(), satellite, propulsion, new HohmannTransfer());
    }

    public Mission(String name, Orbit initialOrbit, Orbit targetOrbit, Satellite satellite,
        Propulsion propulsion, HohmannTransfer calculator)
    {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Mission name cannot be empty.");
        if (initialOrbit == null || targetOrbit == null) throw new IllegalArgumentException("Initial and target orbits cannot be null.");
        if (calculator == null) throw new IllegalArgumentException("Transfer calculator cannot be null.");
        this.name = name.trim();
        this.initialOrbit = initialOrbit;
        this.targetOrbit = targetOrbit;
        this.satellite = satellite;
        this.propulsion = propulsion;
        this.transferCalculator = calculator;
    }

    public String getName() { return name; }
    public Orbit getInitialOrbit() { return initialOrbit; }
    public Orbit getTargetOrbit() { return targetOrbit; }
    public Satellite getSatellite() { return satellite; }
    public Propulsion getPropulsion() { return propulsion; }
    public TransferResult calculateTransfer() { return transferCalculator.calculateTransfer(initialOrbit, targetOrbit); }
    public double getTotalDeltaV() { return calculateTransfer().getTotalDeltaV(); }
    public double getTransferTime() { return calculateTransfer().getTransferTime(); }
}