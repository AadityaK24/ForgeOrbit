public class HohmannTransfer
{
    private final Earth earth;
    private final OrbitalMechanics orbitalMechanics;

    public HohmannTransfer()
    {
        earth = new Earth();
        orbitalMechanics = new OrbitalMechanics();
    }

    public double calculateTransferSemiMajorAxis(
        Orbit initialOrbit,
        Orbit targetOrbit)
    {
        return (initialOrbit.getRadius() + targetOrbit.getRadius()) / 2.0;
    }

    public double calculateFirstBurnDeltaV(
        Orbit initialOrbit,
        Orbit targetOrbit)
    {
        double r1 = initialOrbit.getRadius();
        double r2 = targetOrbit.getRadius();
        double mu = earth.getGravitationalParameter();

        double transferRadius = calculateTransferSemiMajorAxis(
            initialOrbit,
            targetOrbit
        );

        double initialVelocity =
            orbitalMechanics.calculateCircularVelocity(initialOrbit);

        double transferVelocity = Math.sqrt(
            mu * ((2.0 / r1) - (1.0 / transferRadius))
        );

        return Math.abs(transferVelocity - initialVelocity);
    }

    public double calculateSecondBurnDeltaV(
        Orbit initialOrbit,
        Orbit targetOrbit)
    {
        double r1 = initialOrbit.getRadius();
        double r2 = targetOrbit.getRadius();
        double mu = earth.getGravitationalParameter();

        double transferRadius = calculateTransferSemiMajorAxis(
            initialOrbit,
            targetOrbit
        );

        double targetVelocity =
            orbitalMechanics.calculateCircularVelocity(targetOrbit);

        double transferVelocity = Math.sqrt(
            mu * ((2.0 / r2) - (1.0 / transferRadius))
        );

        return Math.abs(targetVelocity - transferVelocity);
    }

    public double calculateTotalDeltaV(
        Orbit initialOrbit,
        Orbit targetOrbit)
    {
        return calculateFirstBurnDeltaV(initialOrbit, targetOrbit)
             + calculateSecondBurnDeltaV(initialOrbit, targetOrbit);
    }

    public double calculateTransferTime(
        Orbit initialOrbit,
        Orbit targetOrbit)
    {
        double transferSemiMajorAxis =
            calculateTransferSemiMajorAxis(initialOrbit, targetOrbit);

        return Math.PI * Math.sqrt(
            Math.pow(transferSemiMajorAxis, 3)
            / earth.getGravitationalParameter()
        );
    }
}