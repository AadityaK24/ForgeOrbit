public class OrbitalMechanics
{
    private final Earth earth;

    public OrbitalMechanics()
    {
        earth = new Earth();
    }

    public double calculateCircularVelocity(Orbit orbit)
    {
        double mu = earth.getGravitationalParameter();
        double radius = orbit.getRadius();

        return Math.sqrt(mu / radius);
    }

    public double calculateOrbitalPeriod(Orbit orbit)
    {
        double mu = earth.getGravitationalParameter();
        double radius = orbit.getRadius();

        return Constants.TWO_PI * Math.sqrt(
            Math.pow(radius, 3) / mu
        );
    }

    public double calculateEscapeVelocity(Orbit orbit)
    {
        double mu = earth.getGravitationalParameter();
        double radius = orbit.getRadius();

        return Math.sqrt((2.0 * mu) / radius);
    }

    public double calculateGravitationalAcceleration(Orbit orbit)
    {
        double mu = earth.getGravitationalParameter();
        double radius = orbit.getRadius();

        return mu / Math.pow(radius, 2);
    }

    public double calculateSpecificOrbitalEnergy(Orbit orbit)
    {
        double mu = earth.getGravitationalParameter();
        double radius = orbit.getRadius();

        return -mu / (2.0 * radius);
    }

    public double calculateVisVivaVelocity(
        Orbit orbit,
        double semiMajorAxis)
    {
        if (semiMajorAxis <= 0)
        {
            throw new IllegalArgumentException(
                "Semi-major axis must be greater than 0."
            );
        }

        double mu = earth.getGravitationalParameter();
        double radius = orbit.getRadius();

        return Math.sqrt(
            mu * ((2.0 / radius) - (1.0 / semiMajorAxis))
        );
    }
}