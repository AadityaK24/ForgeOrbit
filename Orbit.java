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
        if (altitude <= 0)
        {
            throw new IllegalArgumentException("Orbit altitude must be greater than 0.");
        }

        this.altitude = altitude;
        radius = earth.getRadius() + altitude;
    }

    public Earth getEarth()
    {
        return earth;
    }
}