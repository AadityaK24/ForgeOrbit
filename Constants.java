
public final class Constants
{
    // ==========================================
    // FUNDAMENTAL PHYSICAL CONSTANTS
    // ==========================================

    public static final double UNIVERSAL_GRAVITATIONAL_CONSTANT =
        6.67430e-11; // m^3/(kg s^2)

    public static final double STANDARD_GRAVITY =
        9.80665; // m/s^2

    public static final double SPEED_OF_LIGHT =
        299792458.0; // m/s


    // ==========================================
    // TIME CONSTANTS
    // ==========================================

    public static final double SECONDS_PER_MINUTE = 60.0;
    public static final double SECONDS_PER_HOUR = 3600.0;
    public static final double SECONDS_PER_DAY = 86400.0;

    public static final double DAYS_PER_JULIAN_YEAR = 365.25;

    public static final double SECONDS_PER_JULIAN_YEAR =
        DAYS_PER_JULIAN_YEAR * SECONDS_PER_DAY;

    public static final double EARTH_SIDEREAL_DAY_SECONDS =
        86164.09054;

    // Julian Date at the J2000.0 epoch.
    public static final double J2000_JULIAN_DATE = 2451545.0;


    // ==========================================
    // DISTANCE CONVERSIONS
    // ==========================================

    public static final double KM_TO_M = 1000.0;
    public static final double M_TO_KM = 0.001;

    // IAU-defined astronomical unit.
    public static final double AU_TO_M = 149597870700.0;
    public static final double M_TO_AU = 1.0 / AU_TO_M;

    public static final double AU_TO_KM =
        AU_TO_M / KM_TO_M;

    public static final double LIGHT_TIME_PER_AU_SECONDS =
        AU_TO_M / SPEED_OF_LIGHT;


    // ==========================================
    // ANGLE AND MATHEMATICAL CONSTANTS
    // ==========================================

    public static final double DEG_TO_RAD = Math.PI / 180.0;
    public static final double RAD_TO_DEG = 180.0 / Math.PI;

    public static final double TWO_PI = 2.0 * Math.PI;


    // ==========================================
    // SUN
    // ==========================================

    public static final double SUN_RADIUS = 695700000.0; // m

    public static final double SUN_MU =
        1.32712440041279419e20; // m^3/s^2


    // ==========================================
    // MERCURY
    // ==========================================

    public static final double MERCURY_RADIUS = 2439700.0; // m

    public static final double MERCURY_MU =
        2.2031868551e13; // m^3/s^2


    // ==========================================
    // VENUS
    // ==========================================

    public static final double VENUS_RADIUS = 6051800.0; // m

    public static final double VENUS_MU =
        3.2485859200e14; // m^3/s^2


    // ==========================================
    // EARTH
    // ==========================================

    // Volumetric mean radius.
    public static final double EARTH_RADIUS = 6371000.0; // m

    public static final double EARTH_EQUATORIAL_RADIUS =
        6378137.0; // m

    public static final double EARTH_POLAR_RADIUS =
        6356752.0; // m

    public static final double EARTH_MASS =
        5.9722e24; // kg

    // Gravitational parameter GM from JPL DE440.
    // Prefer this value for orbital calculations.
    public static final double EARTH_MU =
        3.98600435507e14; // m^3/s^2


    // ==========================================
    // MOON
    // ==========================================

    public static final double MOON_RADIUS =
        1737400.0; // m

    public static final double MOON_MU =
        4.902800118e12; // m^3/s^2

    public static final double EARTH_MOON_MEAN_DISTANCE =
        384400000.0; // m

    public static final double MOON_ORBITAL_PERIOD_DAYS =
        27.321661;

    public static final double MOON_SYNODIC_PERIOD_DAYS =
        29.53059;


    // ==========================================
    // MARS
    // ==========================================

    public static final double MARS_RADIUS = 3389500.0; // m

    public static final double MARS_MU =
        4.2828375816e13; // m^3/s^2


    // ==========================================
    // JUPITER
    // ==========================================

    public static final double JUPITER_RADIUS =
        69911000.0; // m

    public static final double JUPITER_MU =
        1.2671276410e17; // m^3/s^2


    // ==========================================
    // SATURN
    // ==========================================

    public static final double SATURN_RADIUS =
        58232000.0; // m

    public static final double SATURN_MU =
        3.79405848418e16; // m^3/s^2


    // ==========================================
    // URANUS
    // ==========================================

    public static final double URANUS_RADIUS =
        25362000.0; // m

    public static final double URANUS_MU =
        5.7945564000e15; // m^3/s^2


    // ==========================================
    // NEPTUNE
    // ==========================================

    public static final double NEPTUNE_RADIUS =
        24622000.0; // m

    public static final double NEPTUNE_MU =
        6.83652710058e15; // m^3/s^2


    // Prevent object creation.
    private Constants()
    {
    }
}
