public class Earth
{
    private final double radius;
    private final double mass;
    private final double gravitationalParameter;
    private final double surfaceGravity;

    public Earth()
    {
        radius = Constants.EARTH_RADIUS;
        mass = Constants.EARTH_MASS;
        gravitationalParameter = Constants.EARTH_MU;
        surfaceGravity = Constants.STANDARD_GRAVITY;
    }

    public double getRadius()
    {
        return radius;
    }

    public double getMass()
    {
        return mass;
    }

    public double getGravitationalParameter()
    {
        return gravitationalParameter;
    }

    public double getSurfaceGravity()
    {
        return surfaceGravity;
    }
}