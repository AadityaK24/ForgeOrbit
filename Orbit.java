
public class Orbit
{
    private final Earth earth;

    private double altitude;
    private double radius;

    public Orbit(double altitude)
    {
        earth = new Earth();
        setAltitude(altitude);
    }

    public double getAltitude()
    {
        return altitude;
    }

    public double getRadius()
    {
        return radius;
    }

    public void setAltitude(double altitude)
    {
        if (!Double.isFinite(altitude) || altitude <= 0.0)
        {
            throw new IllegalArgumentException(
                "Orbit altitude must be finite and greater than zero."
            );
        }

        double newRadius = earth.getRadius() + altitude;

        if (!Double.isFinite(newRadius))
        {
            throw new IllegalArgumentException(
                "Calculated orbital radius is invalid."
            );
        }

        this.altitude = altitude;
        this.radius = newRadius;
    }

    public Earth getEarth()
    {
        return earth;
    }
}
