public class Earth extends CelestialBody
{
    public Earth()
    {
        this(Constants.EARTH_RADIUS, Constants.EARTH_MASS, Constants.EARTH_MU);
    }

    public Earth(double radius, double mass, double gravitationalParameter)
    {
        super("Earth", mass, radius, gravitationalParameter, null, "TERRESTRIAL",
              Constants.EARTH_SIDEREAL_DAY_SECONDS / Constants.SECONDS_PER_HOUR,
              23.43928, "#3978C5", false);
    }
}
