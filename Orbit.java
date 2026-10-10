public class Orbit
{
    private final Earth earth;
    private double altitude;
    private double radius;

    // Altitude is in metres above the mean Earth radius.
    public Orbit(double altitude)
    {
        this(new Earth(), altitude);
    }

    public Orbit(Earth earth, double altitude)
    {
        if (earth == null)
        {
            throw new IllegalArgumentException("Earth model cannot be null.");
        }
        if (!Double.isFinite(altitude) || altitude < 0.0)
        {
            throw new IllegalArgumentException("Orbit altitude must be finite and non-negative.");
        }
        this.earth = earth;
        setAltitude(altitude);
    }

    public Earth getEarth() { return earth; }
    public double getAltitude() { return altitude; }
    public double getRadius() { return radius; }

    public void setAltitude(double altitude)
    {
        if (!Double.isFinite(altitude) || altitude < 0.0)
        {
            throw new IllegalArgumentException("Orbit altitude must be finite and non-negative.");
        }
        double computedRadius = earth.getRadius() + altitude;
        if (!Double.isFinite(computedRadius) || computedRadius <= earth.getRadius())
        {
            throw new IllegalArgumentException("Computed orbital radius is invalid.");
        }
        this.altitude = altitude;
        this.radius = computedRadius;
    }

    public double getAltitudeKm() { return altitude * Constants.M_TO_KM; }
    public double getRadiusKm() { return radius * Constants.M_TO_KM; }
}
