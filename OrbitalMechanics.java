public class OrbitalMechanics
{
    private final CelestialBody centralBody;

    public OrbitalMechanics()
    {
        this(new Earth());
    }

    public OrbitalMechanics(Earth earth)
    {
        this((CelestialBody) earth);
    }

    public OrbitalMechanics(CelestialBody centralBody)
    {
        if (centralBody == null)
            throw new IllegalArgumentException("Central body cannot be null.");
        this.centralBody = centralBody;
    }

    /** Legacy accessor retained for Earth-only callers. */
    public Earth getEarth()
    {
        if (!(centralBody instanceof Earth))
            throw new IllegalStateException("This mechanics model is not Earth-centred.");
        return (Earth) centralBody;
    }

    public CelestialBody getCentralBody() { return centralBody; }

    public double calculateCircularVelocity(Orbit orbit)
    {
        validateOrbit(orbit);
        return requireFinitePositive(Math.sqrt(orbit.getCentralBody().getMu() / orbit.getRadius()));
    }

    public double calculateOrbitalVelocity(Orbit orbit)
    {
        return calculateCircularVelocity(orbit);
    }

    public double calculateCircularVelocity(double radiusMetres)
    {
        validateRadius(radiusMetres);
        return requireFinitePositive(Math.sqrt(centralBody.getMu() / radiusMetres));
    }

    public double calculateOrbitalPeriod(Orbit orbit)
    {
        validateOrbit(orbit);
        return requireFinitePositive(Constants.TWO_PI * Math.sqrt(
            Math.pow(orbit.getRadius(), 3.0) / orbit.getCentralBody().getMu()));
    }

    public double calculateOrbitalPeriod(double radiusMetres)
    {
        validateRadius(radiusMetres);
        return requireFinitePositive(Constants.TWO_PI * Math.sqrt(
            Math.pow(radiusMetres, 3.0) / centralBody.getMu()));
    }

    public double calculateEscapeVelocity(Orbit orbit)
    {
        validateOrbit(orbit);
        return requireFinitePositive(Math.sqrt(2.0 * orbit.getCentralBody().getMu() / orbit.getRadius()));
    }

    public double calculateEscapeVelocity(double radiusMetres)
    {
        validateRadius(radiusMetres);
        return requireFinitePositive(Math.sqrt(2.0 * centralBody.getMu() / radiusMetres));
    }

    public double calculateGravitationalAcceleration(Orbit orbit)
    {
        validateOrbit(orbit);
        double radius = orbit.getRadius();
        return requireFinitePositive(orbit.getCentralBody().getMu() / (radius * radius));
    }

    public double calculateGravitationalAcceleration(double radiusMetres)
    {
        validateRadius(radiusMetres);
        return requireFinitePositive(centralBody.getMu() / (radiusMetres * radiusMetres));
    }

    public double calculateSpecificOrbitalEnergy(Orbit orbit)
    {
        validateOrbit(orbit);
        return -orbit.getCentralBody().getMu() / (2.0 * orbit.getRadius());
    }

    public double calculateSpecificOrbitalEnergy(double radiusMetres)
    {
        validateRadius(radiusMetres);
        return -centralBody.getMu() / (2.0 * radiusMetres);
    }

    public double calculateVisVivaVelocity(Orbit orbit, double semiMajorAxis)
    {
        validateOrbit(orbit);
        validateSemiMajorAxis(semiMajorAxis);
        double term = 2.0 / orbit.getRadius() - 1.0 / semiMajorAxis;
        if (!Double.isFinite(term) || term <= 0.0)
            throw new IllegalArgumentException("Radius and semimajor axis do not produce a valid orbital velocity.");
        return requireFinitePositive(Math.sqrt(orbit.getCentralBody().getMu() * term));
    }

    public double calculateVisVivaVelocity(double radiusMetres, double semiMajorAxis)
    {
        validateRadius(radiusMetres);
        validateSemiMajorAxis(semiMajorAxis);
        double term = 2.0 / radiusMetres - 1.0 / semiMajorAxis;
        if (!Double.isFinite(term) || term <= 0.0)
            throw new IllegalArgumentException("Radius and semimajor axis do not produce a valid orbital velocity.");
        return requireFinitePositive(Math.sqrt(centralBody.getMu() * term));
    }

    private void validateOrbit(Orbit orbit)
    {
        if (orbit == null)
            throw new IllegalArgumentException("Orbit cannot be null.");
        validateRadius(orbit.getRadius());
        CelestialBody orbitBody = orbit.getCentralBody();
        double expectedMu = centralBody.getMu();
        double actualMu = orbitBody.getMu();
        if (!centralBody.getName().equalsIgnoreCase(orbitBody.getName())
            || Math.abs(expectedMu - actualMu) > Math.max(expectedMu, actualMu) * 1.0e-12)
            throw new IllegalArgumentException("Orbit and mechanics model use different central bodies.");
    }

    private static void validateRadius(double radius)
    {
        if (!Double.isFinite(radius) || radius <= 0.0)
            throw new IllegalArgumentException("Orbital radius must be finite and positive.");
    }

    private static void validateSemiMajorAxis(double semiMajorAxis)
    {
        if (!Double.isFinite(semiMajorAxis) || semiMajorAxis <= 0.0)
            throw new IllegalArgumentException("Semimajor axis must be finite and positive.");
    }

    private static double requireFinitePositive(double value)
    {
        if (!Double.isFinite(value) || value <= 0.0)
            throw new ArithmeticException("Orbital calculation produced an invalid result.");
        return value;
    }
}
