import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Verified baseline body data and JPL approximate orbital elements for the Solar System. */
public final class SolarSystemCatalog
{
    public static final CelestialBody SUN = new CelestialBody(
        "Sun", Constants.SUN_MASS, Constants.SUN_RADIUS, Constants.SUN_MU,
        null, "STAR", 609.12, 7.25, "#F6B73C", false);

    public static final CelestialBody MERCURY = body("Mercury", Constants.MERCURY_MASS,
        Constants.MERCURY_RADIUS, Constants.MERCURY_MU, elements(
        0.38709843, 0.00000000, 0.20563661, 0.00002123, 7.00559432, -0.00590158,
        252.25166724, 149472.67486623, 77.45771895, 0.15940013, 48.33961819, -0.12214182,
        0, 0, 0, 0), "TERRESTRIAL", 1407.6, 0.034, "#A5A5A5", false);

    public static final CelestialBody VENUS = body("Venus", Constants.VENUS_MASS,
        Constants.VENUS_RADIUS, Constants.VENUS_MU, elements(
        0.72332102, -0.00000026, 0.00676399, -0.00005107, 3.39777545, 0.00043494,
        181.97970850, 58517.81560260, 131.76755713, 0.05679648, 76.67261496, -0.27274174,
        0, 0, 0, 0), "TERRESTRIAL", -5832.5, 177.36, "#D6A36A", false);

    // JPL's compact long-range table provides Earth-Moon barycentre elements; used here as an Earth proxy.
    public static final CelestialBody EARTH = body("Earth", Constants.EARTH_MASS,
        Constants.EARTH_RADIUS, Constants.EARTH_MU, elements(
        1.00000018, -0.00000003, 0.01673163, -0.00003661, -0.00054346, -0.01337178,
        100.46691572, 35999.37306329, 102.93005885, 0.31795260, -5.11260389, -0.24123856,
        0, 0, 0, 0), "TERRESTRIAL", 23.9345, 23.43928, "#3978C5", false);

    public static final CelestialBody MARS = body("Mars", Constants.MARS_MASS,
        Constants.MARS_RADIUS, Constants.MARS_MU, elements(
        1.52371243, 0.00000097, 0.09336511, 0.00009149, 1.85181869, -0.00724757,
        -4.56813164, 19140.29934243, -23.91744784, 0.45223625, 49.71320984, -0.26852431,
        0, 0, 0, 0), "TERRESTRIAL", 24.6229, 25.19, "#C75C3A", false);

    public static final CelestialBody JUPITER = body("Jupiter", Constants.JUPITER_MASS,
        Constants.JUPITER_RADIUS, Constants.JUPITER_MU, elements(
        5.20248019, -0.00002864, 0.04853590, 0.00018026, 1.29861416, -0.00322699,
        34.33479152, 3034.90371757, 14.27495244, 0.18199196, 100.29282654, 0.13024619,
        -0.00012452, 0.06064060, -0.35635438, 38.35125000),
        "GAS_GIANT", 9.925, 3.13, "#C99B73", true);

    public static final CelestialBody SATURN = body("Saturn", Constants.SATURN_MASS,
        Constants.SATURN_RADIUS, Constants.SATURN_MU, elements(
        9.54149883, -0.00003065, 0.05550825, -0.00032044, 2.49424102, 0.00451969,
        50.07571329, 1222.11494724, 92.86136063, 0.54179478, 113.63998702, -0.25015002,
        0.00025899, -0.13434469, 0.87320147, 38.35125000),
        "GAS_GIANT", 10.656, 26.73, "#D6BD82", true);

    public static final CelestialBody URANUS = body("Uranus", Constants.URANUS_MASS,
        Constants.URANUS_RADIUS, Constants.URANUS_MU, elements(
        19.18797948, -0.00020455, 0.04685740, -0.00001550, 0.77298127, -0.00180155,
        314.20276625, 428.49512595, 172.43404441, 0.09266985, 73.96250215, 0.05739699,
        0.00058331, -0.97731848, 0.17689245, 7.67025000),
        "ICE_GIANT", -17.24, 97.77, "#78C9D4", true);

    public static final CelestialBody NEPTUNE = body("Neptune", Constants.NEPTUNE_MASS,
        Constants.NEPTUNE_RADIUS, Constants.NEPTUNE_MU, elements(
        30.06952752, 0.00006447, 0.00895439, 0.00000818, 1.77005520, 0.00022400,
        304.22289287, 218.46515314, 46.68158724, 0.01009938, 131.78635853, -0.00606302,
        -0.00041348, 0.68346318, -0.10162547, 7.67025000),
        "ICE_GIANT", 16.11, 28.32, "#426FE0", true);

    private static final List<CelestialBody> ALL_BODIES;
    private static final List<CelestialBody> PLANETS;
    private static final Map<String, CelestialBody> BY_NAME;

    static
    {
        List<CelestialBody> all = new ArrayList<CelestialBody>();
        all.add(SUN);
        all.add(MERCURY);
        all.add(VENUS);
        all.add(EARTH);
        all.add(MARS);
        all.add(JUPITER);
        all.add(SATURN);
        all.add(URANUS);
        all.add(NEPTUNE);
        ALL_BODIES = Collections.unmodifiableList(all);

        List<CelestialBody> planets = new ArrayList<CelestialBody>(all.subList(1, all.size()));
        PLANETS = Collections.unmodifiableList(planets);

        Map<String, CelestialBody> map = new LinkedHashMap<String, CelestialBody>();
        for (CelestialBody body : all)
            map.put(body.getName().toLowerCase(Locale.ROOT), body);
        BY_NAME = Collections.unmodifiableMap(map);
    }

    private SolarSystemCatalog() { }

    public static List<CelestialBody> getAllBodies() { return ALL_BODIES; }
    public static List<CelestialBody> getPlanets() { return PLANETS; }

    public static CelestialBody getBody(String name)
    {
        if (name == null)
            throw new IllegalArgumentException("Body name cannot be null.");
        CelestialBody body = BY_NAME.get(name.trim().toLowerCase(Locale.ROOT));
        if (body == null)
            throw new IllegalArgumentException("Unknown Solar System body: " + name);
        return body;
    }

    private static OrbitalElements elements(double a, double aRate, double e, double eRate,
            double i, double iRate, double l, double lRate, double peri, double periRate,
            double node, double nodeRate, double b, double c, double s, double f)
    {
        return new OrbitalElements(a, aRate, e, eRate, i, iRate, l, lRate,
            peri, periRate, node, nodeRate, b, c, s, f);
    }

    private static CelestialBody body(String name, double mass, double radius, double mu,
            OrbitalElements elements, String type, double rotationHours, double tilt,
            String color, boolean rings)
    {
        return new CelestialBody(name, mass, radius, mu, elements, type,
            rotationHours, tilt, color, rings);
    }
}
