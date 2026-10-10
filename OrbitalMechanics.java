public class OrbitalMechanics
{
    private final Earth earth;

    public OrbitalMechanics() { this(new Earth()); }

    public OrbitalMechanics(Earth earth)
    {
        if (earth == null)
        {
            throw new IllegalArgumentException("Earth model cannot be null.");
        }
        this.earth = earth;
    }

    public Earth getEarth() { return earth; }

    public double calculateCircularVelocity(Orbit orbit)
    {
        validateOrbit(orbit);
        return requireFinitePositive(Math.sqrt(earth.getGravitationalParameter() / orbit.getRadius()));
    }

    public double calculateOrbitalVelocity(Orbit orbit)
    {
        return calculateCircularVelocity(orbit);
    }

    public double calculateCircularVelocity(double radiusMetres)
    {
        validateRadius(radiusMetres);
        return requireFinitePositive(Math.sqrt(earth.getGravitationalParameter() / radiusMetres));
    }

    public double calculateOrbitalPeriod(Orbit orbit)
    {
        validateOrbit(orbit);
        double period = Constants.TWO_PI * Math.sqrt(
            Math.pow(orbit.getRadius(), 3.0) / earth.getGravitationalParameter());
        return requireFinitePositive(period);
    }

    public double calculateOrbitalPeriod(double radiusMetres)
    {
        validateRadius(radiusMetres);
        double period = Constants.TWO_PI * Math.sqrt(
            Math.pow(radiusMetres, 3.0) / earth.getGravitationalParameter());
        return requireFinitePositive(period);
    }

    public double calculateEscapeVelocity(Orbit orbit)
    {
        validateOrbit(orbit);
        return requireFinitePositive(Math.sqrt(2.0 * earth.getGravitationalParameter() / orbit.getRadius()));
    }

    public double calculateEscapeVelocity(double radiusMetres)
    {
        validateRadius(radiusMetres);
        return requireFinitePositive(Math.sqrt(2.0 * earth.getGravitationalParameter() / radiusMetres));
    }

    public double calculateGravitationalAcceleration(Orbit orbit)
    {
        validateOrbit(orbit);
        return requireFinitePositive(earth.getGravitationalParameter()
            / (orbit.getRadius() * orbit.getRadius()));
    }

    public double calculateGravitationalAcceleration(double radiusMetres)
    {
        validateRadius(radiusMetres);
        return requireFinitePositive(earth.getGravitationalParameter() / (radiusMetres * radiusMetres));
    }

    public double calculateSpecificOrbitalEnergy(Orbit orbit)
    {
        validateOrbit(orbit);
        return -earth.getGravitationalParameter() / (2.0 * orbit.getRadius());
    }

    public double calculateSpecificOrbitalEnergy(double radiusMetres)
    {
        validateRadius(radiusMetres);
        return -earth.getGravitationalParameter() / (2.0 * radiusMetres);
    }

    public double calculateVisVivaVelocity(Orbit orbit, double semiMajorAxis)
    {
        validateOrbit(orbit);
        if (!Double.isFinite(semiMajorAxis) || semiMajorAxis <= 0.0)
        {
            throw new IllegalArgumentException("Semi-major axis must be finite and positive.");
        }
        double velocityTerm = (2.0 / orbit.getRadius()) - (1.0 / semiMajorAxis);
        if (!Double.isFinite(velocityTerm) || velocityTerm <= 0.0)
        {
            throw new IllegalArgumentException("Radius and semi-major axis do not produce a valid orbital velocity.");
        }
        return requireFinitePositive(Math.sqrt(earth.getGravitationalParameter() * velocityTerm));
    }

    public double calculateVisVivaVelocity(double radiusMetres, double semiMajorAxis)
    {
        validateRadius(radiusMetres);
        if (!Double.isFinite(semiMajorAxis) || semiMajorAxis <= 0.0)
        {
            throw new IllegalArgumentException("Semi-major axis must be finite and positive.");
        }
        double term = 2.0 / radiusMetres - 1.0 / semiMajorAxis;
        if (!Double.isFinite(term) || term <= 0.0)
        {
            throw new IllegalArgumentException("Radius and semi-major axis do not produce a valid orbital velocity.");
        }
        return requireFinitePositive(Math.sqrt(earth.getGravitationalParameter() * term));
    }

    private void validateOrbit(Orbit orbit)
    {
        if (orbit == null) throw new IllegalArgumentException("Orbit cannot be null.");
        validateRadius(orbit.getRadius());
    }

    private void validateRadius(double radius)
    {
        if (!Double.isFinite(radius) || radius <= 0.0)
        {
            throw new IllegalArgumentException("Orbital radius must be finite and positive.");
        }
    }

    private double requireFinitePositive(double value)
    {
        if (!Double.isFinite(value) || value <= 0.0)
        {
            throw new ArithmeticException("Orbital calculation produced an invalid result.");
        }
        return value;
    }
}
