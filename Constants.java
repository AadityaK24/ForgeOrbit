public final class Constants
{
    public static final double UNIVERSAL_GRAVITATIONAL_CONSTANT = 6.67430e-11;
    public static final double STANDARD_GRAVITY = 9.80665;
    public static final double SPEED_OF_LIGHT = 299792458.0;

    public static final double SECONDS_PER_MINUTE = 60.0;
    public static final double SECONDS_PER_HOUR = 3600.0;
    public static final double SECONDS_PER_DAY = 86400.0;
    public static final double DAYS_PER_JULIAN_YEAR = 365.25;
    public static final double SECONDS_PER_JULIAN_YEAR = DAYS_PER_JULIAN_YEAR * SECONDS_PER_DAY;
    public static final double EARTH_SIDEREAL_DAY_SECONDS = 86164.09054;
    public static final double J2000_JULIAN_DATE = 2451545.0;

    public static final double KM_TO_M = 1000.0;
    public static final double M_TO_KM = 0.001;
    public static final double AU_TO_M = 149597870700.0;
    public static final double M_TO_AU = 1.0 / AU_TO_M;
    public static final double AU_TO_KM = AU_TO_M / KM_TO_M;
    public static final double LIGHT_TIME_PER_AU_SECONDS = AU_TO_M / SPEED_OF_LIGHT;

    public static final double DEG_TO_RAD = Math.PI / 180.0;
    public static final double RAD_TO_DEG = 180.0 / Math.PI;
    public static final double TWO_PI = 2.0 * Math.PI;
    public static final double J2000_ECLIPTIC_OBLIQUITY_DEG = 23.43928;

    // Solar physical properties. The Solar System catalog is the body-data source of truth.
    public static final double SUN_MASS = 1.98847e30; // kg, nominal approximate mass
    public static final double SUN_RADIUS = 695700000.0; // m
    public static final double SUN_MU = 1.32712440041279419e20; // m^3/s^2
    public static final double SUN_LUMINOSITY = 3.828e26; // W
    public static final double SUN_EFFECTIVE_TEMPERATURE = 5772.0; // K

    public static final double MERCURY_MASS = 3.30103e23; // kg
    public static final double MERCURY_RADIUS = 2439700.0; // m
    public static final double MERCURY_MU = 2.2031868551e13; // m^3/s^2

    public static final double VENUS_MASS = 4.86731e24; // kg
    public static final double VENUS_RADIUS = 6051800.0; // m
    public static final double VENUS_MU = 3.2485859200e14; // m^3/s^2

    public static final double EARTH_RADIUS = 6371000.0; // m, volumetric mean radius
    public static final double EARTH_EQUATORIAL_RADIUS = 6378137.0; // m
    public static final double EARTH_POLAR_RADIUS = 6356752.0; // m
    public static final double EARTH_MASS = 5.9722e24; // kg
    public static final double EARTH_MU = 3.98600435507e14; // m^3/s^2

    public static final double MOON_MASS = 7.342e22; // kg
    public static final double MOON_RADIUS = 1737400.0; // m
    public static final double MOON_MU = 4.902800118e12; // m^3/s^2
    public static final double EARTH_MOON_MEAN_DISTANCE = 384400000.0; // m
    public static final double MOON_ORBITAL_PERIOD_DAYS = 27.321661;
    public static final double MOON_SYNODIC_PERIOD_DAYS = 29.53059;

    public static final double MARS_MASS = 6.41691e23; // kg
    public static final double MARS_RADIUS = 3389500.0; // m
    public static final double MARS_MU = 4.2828375816e13; // m^3/s^2

    public static final double JUPITER_MASS = 1.898125e27; // kg
    public static final double JUPITER_RADIUS = 69911000.0; // m
    public static final double JUPITER_MU = 1.2671276410e17; // m^3/s^2

    public static final double SATURN_MASS = 5.68317e26; // kg
    public static final double SATURN_RADIUS = 58232000.0; // m
    public static final double SATURN_MU = 3.79405848418e16; // m^3/s^2

    public static final double URANUS_MASS = 8.68099e25; // kg
    public static final double URANUS_RADIUS = 25362000.0; // m
    public static final double URANUS_MU = 5.7945564000e15; // m^3/s^2

    public static final double NEPTUNE_MASS = 1.024092e26; // kg
    public static final double NEPTUNE_RADIUS = 24622000.0; // m
    public static final double NEPTUNE_MU = 6.83652710058e15; // m^3/s^2


    // Legacy compatibility constants used by earlier ForgeOrbit V3/V4 classes.
    public static final double MERCURY_SEMI_MAJOR_AXIS = 0.387098 * AU_TO_M;
    public static final double MERCURY_LONGITUDE_J2000_DEG = 252.25084;
    public static final double VENUS_SEMI_MAJOR_AXIS = 0.723332 * AU_TO_M;
    public static final double VENUS_LONGITUDE_J2000_DEG = 181.97973;
    public static final double EARTH_SEMI_MAJOR_AXIS = 1.000000 * AU_TO_M;
    public static final double EARTH_LONGITUDE_J2000_DEG = 100.46435;
    public static final double MARS_SEMI_MAJOR_AXIS = 1.523679 * AU_TO_M;
    public static final double MARS_LONGITUDE_J2000_DEG = 355.45332;
    public static final double JUPITER_SEMI_MAJOR_AXIS = 5.20260 * AU_TO_M;
    public static final double JUPITER_LONGITUDE_J2000_DEG = 34.40438;
    public static final double SATURN_SEMI_MAJOR_AXIS = 9.55491 * AU_TO_M;
    public static final double SATURN_LONGITUDE_J2000_DEG = 49.94432;
    public static final double URANUS_SEMI_MAJOR_AXIS = 19.2184 * AU_TO_M;
    public static final double URANUS_LONGITUDE_J2000_DEG = 313.23218;
    public static final double NEPTUNE_SEMI_MAJOR_AXIS = 30.1104 * AU_TO_M;
    public static final double NEPTUNE_LONGITUDE_J2000_DEG = 304.88003;

    private Constants()
    {
    }
}
