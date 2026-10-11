public class HohmannTransfer
{
    private final CelestialBody centralBody;
    private final OrbitalMechanics mechanics;

    public HohmannTransfer()
    {
        this(new Earth());
    }

    public HohmannTransfer(Earth earth)
    {
        this((CelestialBody) earth, new OrbitalMechanics(earth));
    }

    public HohmannTransfer(CelestialBody centralBody)
    {
        this(centralBody, new OrbitalMechanics(centralBody));
    }

    public HohmannTransfer(Earth earth, OrbitalMechanics mechanics)
    {
        this((CelestialBody) earth, mechanics);
    }

    public HohmannTransfer(CelestialBody centralBody, OrbitalMechanics mechanics)
    {
        if (centralBody == null || mechanics == null)
            throw new IllegalArgumentException("Central body and orbital mechanics cannot be null.");
        double muA = centralBody.getMu();
        double muB = mechanics.getCentralBody().getMu();
        if (Math.abs(muA - muB) > Math.max(muA, muB) * 1.0e-12)
            throw new IllegalArgumentException("Central body and mechanics model must agree.");
        this.centralBody = centralBody;
        this.mechanics = mechanics;
    }

    public TransferResult calculateTransfer(Orbit initialOrbit, Orbit targetOrbit)
    {
        validateOrbit(initialOrbit, "Initial orbit");
        validateOrbit(targetOrbit, "Target orbit");
        double mu = centralBody.getMu();
        requireMatchingBody(initialOrbit);
        requireMatchingBody(targetOrbit);

        double r1 = initialOrbit.getRadius();
        double r2 = targetOrbit.getRadius();
        double semiMajorAxis = (r1 + r2) / 2.0;
        if (Math.abs(r2 - r1) <= Math.max(r1, r2) * 1.0e-12)
            return new TransferResult(semiMajorAxis, 0.0, 0.0, 0.0, 0.0);

        double circularVelocity1 = Math.sqrt(mu / r1);
        double circularVelocity2 = Math.sqrt(mu / r2);
        double transferVelocity1 = Math.sqrt(mu * (2.0 / r1 - 1.0 / semiMajorAxis));
        double transferVelocity2 = Math.sqrt(mu * (2.0 / r2 - 1.0 / semiMajorAxis));
        double firstBurn = Math.abs(transferVelocity1 - circularVelocity1);
        double secondBurn = Math.abs(circularVelocity2 - transferVelocity2);
        double totalDeltaV = firstBurn + secondBurn;
        double transferTime = Math.PI * Math.sqrt(
            semiMajorAxis * semiMajorAxis * semiMajorAxis / mu);

        if (!Double.isFinite(totalDeltaV) || !Double.isFinite(transferTime))
            throw new ArithmeticException("Hohmann transfer result is invalid.");
        return new TransferResult(semiMajorAxis, firstBurn, secondBurn, totalDeltaV, transferTime);
    }

    public TransferResult calculateHohmannTransfer(Orbit initialOrbit, Orbit targetOrbit)
    {
        return calculateTransfer(initialOrbit, targetOrbit);
    }

    /** Legacy dashboard API: returns only the departure burn delta-v in m/s. */
    public double calculateFirstBurnDeltaV(Orbit initialOrbit, Orbit targetOrbit)
    {
        return calculateTransfer(initialOrbit, targetOrbit).getFirstBurnDeltaV();
    }

    /** Legacy dashboard API: returns only the arrival circularisation burn delta-v in m/s. */
    public double calculateSecondBurnDeltaV(Orbit initialOrbit, Orbit targetOrbit)
    {
        return calculateTransfer(initialOrbit, targetOrbit).getSecondBurnDeltaV();
    }

    /** Legacy dashboard API: returns the ideal transfer time in seconds. */
    public double calculateTransferTime(Orbit initialOrbit, Orbit targetOrbit)
    {
        return calculateTransfer(initialOrbit, targetOrbit).getTransferTime();
    }

    public OrbitalMechanics getOrbitalMechanics() { return mechanics; }

    /** Legacy accessor retained for existing Earth-orbit callers. */
    public Earth getEarth()
    {
        if (!(centralBody instanceof Earth))
            throw new IllegalStateException("This transfer is not Earth-centred.");
        return (Earth) centralBody;
    }

    public CelestialBody getCentralBody() { return centralBody; }

    private void validateOrbit(Orbit orbit, String label)
    {
        if (orbit == null)
            throw new IllegalArgumentException(label + " cannot be null.");
        if (!Double.isFinite(orbit.getRadius()) || orbit.getRadius() <= 0.0)
            throw new IllegalArgumentException(label + " radius is invalid.");
    }

    private void requireMatchingBody(Orbit orbit)
    {
        CelestialBody orbitBody = orbit.getCentralBody();
        double expectedMu = centralBody.getMu();
        double actualMu = orbitBody.getMu();
        if (!centralBody.getName().equalsIgnoreCase(orbitBody.getName())
            || Math.abs(expectedMu - actualMu) > Math.max(expectedMu, actualMu) * 1.0e-12)
            throw new IllegalArgumentException("Both orbits must use the transfer's central body.");
    }
}
