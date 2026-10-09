
public class OrbitalMechanics
{
    private final Earth earth;

    public OrbitalMechanics()
    {
        earth = new Earth();
    }

    public double calculateCircularVelocity(Orbit orbit)
    {
        validateOrbit(orbit);

        double velocity = Math.sqrt(
            earth.getGravitationalParameter()
            / orbit.getRadius()
        );

        return requireFiniteResult(velocity);
    }

    public double calculateOrbitalPeriod(Orbit orbit)
    {
        validateOrbit(orbit);

        double radius = orbit.getRadius();
        double mu = earth.getGravitationalParameter();

        double period = Constants.TWO_PI
            * Math.sqrt(radius * radius * radius / mu);

        return requireFiniteResult(period);
    }

    public double calculateEscapeVelocity(Orbit orbit)
    {
        validateOrbit(orbit);

        double velocity = Math.sqrt(
            2.0 * earth.getGravitationalParameter()
            / orbit.getRadius()
        );

        return requireFiniteResult(velocity);
    }

    public double calculateGravitationalAcceleration(Orbit orbit)
    {
        validateOrbit(orbit);

        double radius = orbit.getRadius();

        double gravity = earth.getGravitationalParameter()
            / (radius * radius);

        return requireFiniteResult(gravity);
    }

    public double calculateSpecificOrbitalEnergy(Orbit orbit)
    {
        validateOrbit(orbit);

        double energy =
            -earth.getGravitationalParameter()
            / (2.0 * orbit.getRadius());

        if (!Double.isFinite(energy))
        {
            throw new ArithmeticException(
                "Specific orbital energy is not finite."
            );
        }

        return energy;
    }

    public double calculateVisVivaVelocity(
        Orbit orbit,
        double semiMajorAxis)
    {
        validateOrbit(orbit);

        if (!Double.isFinite(semiMajorAxis)
                || semiMajorAxis <= 0.0)
        {
            throw new IllegalArgumentException(
                "Semi-major axis must be finite and positive."
            );
        }

        double radius = orbit.getRadius();

        double velocityTerm =
            (2.0 / radius) - (1.0 / semiMajorAxis);

        if (!Double.isFinite(velocityTerm)
                || velocityTerm <= 0.0)
        {
            throw new IllegalArgumentException(
                "The supplied radius and semi-major axis "
                + "do not produce a valid positive orbital velocity."
            );
        }

        double velocity = Math.sqrt(
            earth.getGravitationalParameter() * velocityTerm
        );

        return requireFiniteResult(velocity);
    }

    private void validateOrbit(Orbit orbit)
    {
        if (orbit == null)
        {
            throw new IllegalArgumentException(
                "Orbit cannot be null."
            );
        }

        if (!Double.isFinite(orbit.getRadius())
                || orbit.getRadius() <= 0.0)
        {
            throw new IllegalArgumentException(
                "Orbital radius must be finite and positive."
            );
        }
    }

    private double requireFiniteResult(double value)
    {
        if (!Double.isFinite(value) || value <= 0.0)
        {
            throw new ArithmeticException(
                "Orbital calculation produced an invalid result."
            );
        }

        return value;
    }
}
