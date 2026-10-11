public final class PlanetaryState
{
    private final CelestialBody body;
    private final double julianDate;
    private final Vector3 positionMetres;
    private final Vector3 velocityMetresPerSecond;

    public PlanetaryState(CelestialBody body, double julianDate,
                          Vector3 positionMetres, Vector3 velocityMetresPerSecond)
    {
        if (body == null || positionMetres == null || velocityMetresPerSecond == null)
            throw new IllegalArgumentException("Body, position and velocity cannot be null.");
        if (!Double.isFinite(julianDate))
            throw new IllegalArgumentException("Julian date must be finite.");
        this.body = body;
        this.julianDate = julianDate;
        this.positionMetres = positionMetres;
        this.velocityMetresPerSecond = velocityMetresPerSecond;
    }

    public CelestialBody getBody() { return body; }
    public double getJulianDate() { return julianDate; }
    public Vector3 getPosition() { return positionMetres; }
    public Vector3 getPositionMetres() { return positionMetres; }
    public Vector3 getVelocity() { return velocityMetresPerSecond; }
    public Vector3 getVelocityMetresPerSecond() { return velocityMetresPerSecond; }
    public double getDistanceFromSunMetres() { return positionMetres.magnitude(); }
    public double getDistanceFromSunAu() { return getDistanceFromSunMetres() * Constants.M_TO_AU; }
    public double getSpeedMetresPerSecond() { return velocityMetresPerSecond.magnitude(); }
}
