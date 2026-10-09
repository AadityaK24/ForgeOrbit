
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
        validateOrbits(initialOrbit, targetOrbit);

        return (initialOrbit.getRadius()
            + targetOrbit.getRadius()) / 2.0;
    }

    public double calculateFirstBurnDeltaV(
        Orbit initialOrbit,
        Orbit targetOrbit)
    {
        validateOrbits(initialOrbit, targetOrbit);

        double transferAxis =
            calculateTransferSemiMajorAxis(
                initialOrbit,
                targetOrbit
            );

        double initialVelocity =
            orbitalMechanics.calculateCircularVelocity(
                initialOrbit
            );

        double transferVelocity =
            orbitalMechanics.calculateVisVivaVelocity(
                initialOrbit,
                transferAxis
            );

        return Math.abs(transferVelocity - initialVelocity);
    }

    public double calculateSecondBurnDeltaV(
        Orbit initialOrbit,
        Orbit targetOrbit)
    {
        validateOrbits(initialOrbit, targetOrbit);

        double transferAxis =
            calculateTransferSemiMajorAxis(
                initialOrbit,
                targetOrbit
            );

        double targetVelocity =
            orbitalMechanics.calculateCircularVelocity(
                targetOrbit
            );

        double transferVelocity =
            orbitalMechanics.calculateVisVivaVelocity(
                targetOrbit,
                transferAxis
            );

        return Math.abs(targetVelocity - transferVelocity);
    }

    public double calculateTotalDeltaV(
        Orbit initialOrbit,
        Orbit targetOrbit)
    {
        double firstBurn =
            calculateFirstBurnDeltaV(initialOrbit, targetOrbit);

        double secondBurn =
            calculateSecondBurnDeltaV(initialOrbit, targetOrbit);

        double total = firstBurn + secondBurn;

        if (!Double.isFinite(total))
        {
            throw new ArithmeticException(
                "Total transfer delta-v is invalid."
            );
        }

        return total;
    }

    public double calculateTransferTime(
        Orbit initialOrbit,
        Orbit targetOrbit)
    {
        double semiMajorAxis =
            calculateTransferSemiMajorAxis(
                initialOrbit,
                targetOrbit
            );

        double time = Math.PI * Math.sqrt(
            semiMajorAxis * semiMajorAxis * semiMajorAxis
            / earth.getGravitationalParameter()
        );

        if (!Double.isFinite(time) || time <= 0.0)
        {
            throw new ArithmeticException(
                "Transfer time is invalid."
            );
        }

        return time;
    }

    private void validateOrbits(
        Orbit initialOrbit,
        Orbit targetOrbit)
    {
        if (initialOrbit == null || targetOrbit == null)
        {
            throw new IllegalArgumentException(
                "Initial and target orbits cannot be null."
            );
        }

        if (!Double.isFinite(initialOrbit.getRadius())
                || !Double.isFinite(targetOrbit.getRadius())
                || initialOrbit.getRadius() <= 0.0
                || targetOrbit.getRadius() <= 0.0)
        {
            throw new IllegalArgumentException(
                "Both orbital radii must be finite and positive."
            );
        }
    }
}
