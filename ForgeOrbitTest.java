import java.time.LocalDate;
import java.time.LocalDateTime;

/** Independent sanity checks for ForgeOrbit V5 physics models. */
public final class ForgeOrbitTest
{
    private static int passed = 0;
    private static int failed = 0;

    private ForgeOrbitTest() { }

    public static void main(String[] args)
    {
        System.out.println("=== ForgeOrbit V5 Physics Validation ===");
        testEarthOrbitPhysics();
        testHohmannTransfer();
        testPropulsionAndRocketEquation();
        testLunarTransfer();
        testInterplanetaryTransfer();
        testPlanetaryCatalog();
        System.out.println("\nRESULT: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) System.exit(1);
    }

    private static void testEarthOrbitPhysics()
    {
        section("Earth orbit physics");
        Earth earth = new Earth();
        OrbitalMechanics mechanics = new OrbitalMechanics(earth);
        Orbit leo = new Orbit(earth, 400000.0);
        checkNear("400 km circular speed", mechanics.calculateCircularVelocity(leo), 7668.6, 0.003);
        checkNear("400 km orbital period", mechanics.calculateOrbitalPeriod(leo), 5545.0, 0.004);
        checkNear("Earth surface gravity", earth.getSurfaceGravity(), 9.82, 0.003);
        checkNear("Earth escape velocity", earth.getEscapeVelocity(), 11186.0, 0.003);
        checkNear("Circular-orbit specific energy", mechanics.calculateSpecificOrbitalEnergy(leo),
            -earth.getMu() / (2.0 * leo.getRadius()), 1.0e-10);
        checkThrows("Reject zero-altitude orbit", new Runnable() {
            public void run() { new Orbit(earth, 0.0); }
        });
    }

    private static void testHohmannTransfer()
    {
        section("Hohmann transfer");
        Earth earth = new Earth();
        HohmannTransfer transfer = new HohmannTransfer(earth);
        Orbit leo = new Orbit(earth, 200000.0);
        Orbit geo = new Orbit(earth, 35786000.0);
        TransferResult result = transfer.calculateTransfer(leo, geo);
        check("Total delta-v equals sum of burns", Math.abs(result.getTotalDeltaV()
            - result.getFirstBurnDeltaV() - result.getSecondBurnDeltaV()) < 1.0e-8);
        checkRange("LEO-to-GEO delta-v", result.getTotalDeltaV(), 3900.0, 4300.0);
        checkRange("LEO-to-GEO transfer time", result.getTransferTime(), 18000.0, 24000.0);
        TransferResult same = transfer.calculateTransfer(leo, new Orbit(earth, 200000.0));
        checkNear("Same-orbit transfer delta-v", same.getTotalDeltaV(), 0.0, 1.0e-12);
        checkNear("Same-orbit transfer time", same.getTransferTime(), 0.0, 1.0e-12);
        checkThrows("Reject transfer with different central body", new Runnable() {
            public void run() {
                transfer.calculateTransfer(leo, new Orbit(SolarSystemCatalog.MARS, 100000.0));
            }
        });
    }

    private static void testPropulsionAndRocketEquation()
    {
        section("Propulsion and Tsiolkovsky rocket equation");
        Propulsion engine = new Propulsion("Test engine", 300.0, 20000.0, 0.95);
        PropellantCalculator calculator = new PropellantCalculator();
        checkNear("Effective exhaust velocity", engine.calculateExhaustVelocity(), 2941.995, 1.0e-9);
        checkNear("Effective thrust", engine.getEffectiveThrust(), 19000.0, 1.0e-12);
        checkNear("Mass flow rate", engine.calculateMassFlowRate(),
            19000.0 / (300.0 * Constants.STANDARD_GRAVITY), 1.0e-10);
        double dryMass = 1000.0;
        double targetDeltaV = 2500.0;
        double propellant = calculator.calculateRequiredPropellant(targetDeltaV, dryMass, engine);
        checkNear("Rocket-equation round trip", calculator.calculateAvailableDeltaV(dryMass, propellant, engine),
            targetDeltaV, 1.0e-9);
        checkNear("Final mass relation", calculator.calculateFinalMass(dryMass + propellant,
            targetDeltaV, engine), dryMass, 1.0e-9);
        checkNear("Zero propellant gives zero delta-v", calculator.calculateAvailableDeltaV(dryMass, 0.0, engine),
            0.0, 1.0e-12);
        checkThrows("Reject negative propellant", new Runnable() {
            public void run() { calculator.calculateAvailableDeltaV(dryMass, -1.0, engine); }
        });
    }

    private static void testLunarTransfer()
    {
        section("Lunar transfer estimate");
        LunarMissionCalculator calculator = new LunarMissionCalculator();
        LunarMissionResult result = calculator.calculateTransfer(200000.0);
        checkRange("Lunar departure delta-v", result.getDepartureDeltaV(), 2500.0, 3400.0);
        checkRange("Lunar capture delta-v", result.getLunarCaptureDeltaV(), 400.0, 1500.0);
        checkRange("Lunar transfer duration", result.getTransferTimeSeconds() / 3600.0, 60.0, 150.0);
        checkNear("Lunar total delta-v sum", result.getTotalDeltaV(),
            result.getDepartureDeltaV() + result.getLunarCaptureDeltaV(), 1.0e-10);
        checkRange("New moon phase fraction", calculator.calculateApproximateMoonPhase(LocalDate.of(2000, 1, 6)),
            0.0, 0.001);
        checkThrows("Reject too-low parking orbit", new Runnable() {
            public void run() { calculator.calculateTransfer(50000.0); }
        });
    }

    private static void testInterplanetaryTransfer()
    {
        section("Interplanetary transfer estimate");
        InterplanetaryTransfer calculator = new InterplanetaryTransfer();
        InterplanetaryTransferResult result = calculator.calculateTransfer("Earth", "Mars",
            LocalDateTime.of(2026, 1, 1, 12, 0));
        check("Origin is Earth", "Earth".equals(result.getOrigin()));
        check("Destination is Mars", "Mars".equals(result.getDestination()));
        checkRange("Earth-Mars Hohmann coast", result.getTransferTimeDays(), 150.0, 300.0);
        check("Total delta-v is finite and positive", Double.isFinite(result.getTotalDeltaV())
            && result.getTotalDeltaV() > 0.0);
        check("Launch wait is non-negative", result.getLaunchWaitSeconds() >= 0.0);
        checkThrows("Reject same origin and destination", new Runnable() {
            public void run() { calculator.calculateTransfer("Earth", "Earth"); }
        });
        checkThrows("Reject unknown planet", new Runnable() {
            public void run() { calculator.calculateTransfer("Krypton", "Mars"); }
        });
    }

    private static void testPlanetaryCatalog()
    {
        section("Planetary catalog and ephemeris");
        check("Catalog contains Sun plus eight planets", SolarSystemCatalog.getAllBodies().size() == 9);
        check("Catalog contains eight planets", SolarSystemCatalog.getPlanets().size() == 8);
        checkNear("Earth semi-major axis", SolarSystemCatalog.EARTH.getSemiMajorAxis(),
            Constants.AU_TO_M, 0.002);
        double phase = PlanetaryEphemeris.getPhaseAngleRadians("Earth", "Mars",
            LocalDateTime.of(2026, 1, 1, 12, 0));
        check("Earth-Mars phase is finite and normalized", Double.isFinite(phase) && phase >= 0.0
            && phase < Constants.TWO_PI);
        checkThrows("Reject unknown catalog body", new Runnable() {
            public void run() { SolarSystemCatalog.getBody("Krypton"); }
        });
    }

    private static void section(String name) { System.out.println("\n-- " + name + " --"); }

    private static void check(String name, boolean condition)
    {
        if (condition) { passed++; System.out.println("PASS: " + name); }
        else { failed++; System.out.println("FAIL: " + name); }
    }

    private static void checkNear(String name, double actual, double expected, double relativeTolerance)
    {
        boolean ok = Double.isFinite(actual) && Math.abs(actual - expected)
            <= Math.max(Math.abs(expected), 1.0) * relativeTolerance;
        check(name + " (actual=" + actual + ", expected=" + expected + ")", ok);
    }

    private static void checkRange(String name, double actual, double minimum, double maximum)
    {
        check(name + " (actual=" + actual + ")", Double.isFinite(actual)
            && actual >= minimum && actual <= maximum);
    }

    private static void checkThrows(String name, Runnable operation)
    {
        boolean threw = false;
        try { operation.run(); }
        catch (IllegalArgumentException | IllegalStateException expected) { threw = true; }
        check(name, threw);
    }
}
