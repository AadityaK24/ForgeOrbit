public class Earth
{
    private final double radius;
    private final double mass;
    private final double gravitationalParameter;
    private final double surfaceGravity;

    public Earth()
    {
        this(Constants.EARTH_RADIUS, Constants.EARTH_MASS, Constants.EARTH_MU);
    }

    public Earth(double radius, double mass, double gravitationalParameter)
    {
        requirePositiveFinite(radius, "Earth radius");
        requirePositiveFinite(mass, "Earth mass");
        requirePositiveFinite(gravitationalParameter, "Gravitational parameter");
        this.radius = radius;
        this.mass = mass;
        this.gravitationalParameter = gravitationalParameter;
        this.surfaceGravity = gravitationalParameter / (radius * radius);
    }

    public double getRadius() { return radius; }
    public double getMass() { return mass; }
    public double getGravitationalParameter() { return gravitationalParameter; }
    public double getSurfaceGravity() { return surfaceGravity; }

    private static void requirePositiveFinite(double value, String label)
    {
        if (!Double.isFinite(value) || value <= 0.0)
        {
            throw new IllegalArgumentException(label + " must be finite and positive.");
        }
    }
}
