public final class Constants
{
    // Earth physical constants
    public static final double EARTH_RADIUS = 6_371_000.0;          // m
    public static final double EARTH_MASS = 5.97219e24;             // kg
    public static final double EARTH_MU = 3.986004418e14;           // m^3/s^2

    // Standard gravitational acceleration
    public static final double STANDARD_GRAVITY = 9.80665;          // m/s^2

    // Time constants
    public static final double SECONDS_PER_MINUTE = 60.0;
    public static final double SECONDS_PER_HOUR = 3600.0;
    public static final double SECONDS_PER_DAY = 86400.0;

    // Unit conversions
    public static final double KM_TO_M = 1000.0;
    public static final double M_TO_KM = 0.001;

    // Angle conversions
    public static final double DEG_TO_RAD = Math.PI / 180.0;
    public static final double RAD_TO_DEG = 180.0 / Math.PI;

    // Mathematical constants
    public static final double TWO_PI = 2.0 * Math.PI;

    private Constants()
    {
        // Prevent object creation
    }
}