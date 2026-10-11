/** Smoke and numerical-regression checks for ForgeOrbitTimeStepEngine. */
public final class ForgeOrbitTimeStepEngineTest
{
    private static int passed;
    private static int failed;

    private ForgeOrbitTimeStepEngineTest() { }

    public static void main(String[] args)
    {
        testEarthOrbitConservation();
        testHohmannArrival();
        testLunarPropagation();
        testInterplanetaryPropagation();
        testLunarOrbitCapture();
        testInterplanetaryOrbitCapture();
        System.out.println("ForgeOrbit timestep tests: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) System.exit(1);
    }

    private static void testEarthOrbitConservation()
    {
        ForgeOrbitTimeStepEngine engine = ForgeOrbitTimeStepEngine.earthOrbit(400000.0);
        double energyStart = engine.getPrimarySpecificEnergyJPerKg();
        ForgeOrbitTimeStepEngine.Snapshot last = engine.step(20000.0);
        double energyEnd = last.getSpecificEnergyJPerKg();
        double relativeDrift = Math.abs((energyEnd - energyStart) / energyStart);
        check("Earth orbit advances time", last.getElapsedSeconds() == 20000.0);
        check("Earth orbit reports actual integration substeps", last.getStepCount() >= 4000);
        check("Earth orbit remains bounded", last.getRadiusMetres() > 6.0e6 && last.getRadiusMetres() < 8.0e6);
        check("Earth orbit period is derived from live state", Double.isFinite(last.getOsculatingOrbitalPeriodSeconds()));
        check("RK4 Earth orbit relative energy drift < 1e-8", relativeDrift < 1.0e-8);
        boolean rejectedLargeStep = false;
        try { engine.setIntegrationStepSeconds(100000.0); }
        catch (IllegalArgumentException expected) { rejectedLargeStep = true; }
        check("Unsafe integration timestep is rejected", rejectedLargeStep);
    }

    private static void testHohmannArrival()
    {
        ForgeOrbitTimeStepEngine engine = ForgeOrbitTimeStepEngine.hohmannTransfer(200000.0, 35786000.0);
        ForgeOrbitTimeStepEngine.Snapshot last = engine.step(25000.0);
        check("Hohmann transfer reaches target orbit", last.isTransferComplete());
        check("Hohmann injection burn is accounted for", contains(engine.getEvents(), "injection burn"));
        check("Hohmann automatic circularization burn is logged",
            contains(engine.getEvents(), "circularization burn"));
        check("Hohmann path differs from initial point", engine.getActualTrajectory().size() > 1);
        check("Hohmann target-radius error below 1 m after circularization",
            Math.abs(last.getRadiusMetres() - (Constants.EARTH_RADIUS + 35786000.0)) < 1.0);
    }

    private static void testLunarPropagation()
    {
        ForgeOrbitTimeStepEngine engine = ForgeOrbitTimeStepEngine.lunarTransfer(200000.0);
        Vector3 start = engine.getPositionMetres();
        Vector3 moonStart = engine.getTargetPositionMetres();
        double moonOrbitRadius = moonStart.magnitude();
        ForgeOrbitTimeStepEngine.Snapshot last = engine.step(86400.0);
        check("Lunar simulation advances a full day", Math.abs(last.getElapsedSeconds() - 86400.0) < 1.0e-8);
        check("Lunar position is time-stepped", last.getPositionMetres().distanceTo(start) > 1.0e6);
        check("Lunar distance is finite", Double.isFinite(last.getTargetDistanceMetres()));
        check("Lunar state remains finite", Double.isFinite(last.getSpeedMetresPerSecond()));
        check("Moon stays on its prescribed circular orbit", Math.abs(engine.getTargetPositionMetres().magnitude() - moonOrbitRadius) < 1.0);
    }

    private static void testInterplanetaryPropagation()
    {
        ForgeOrbitTimeStepEngine engine = ForgeOrbitTimeStepEngine.interplanetaryTransfer(
            "Earth", "Mars", Constants.J2000_JULIAN_DATE);
        Vector3 start = engine.getPositionMetres();
        ForgeOrbitTimeStepEngine.Snapshot last = engine.step(Constants.SECONDS_PER_DAY);
        check("Interplanetary simulation advances one day", Math.abs(last.getElapsedSeconds()
            - Constants.SECONDS_PER_DAY) < 1.0e-7);
        check("Interplanetary spacecraft position is time-stepped", last.getPositionMetres().distanceTo(start) > 1.0e8);
        check("Interplanetary target distance is finite", Double.isFinite(last.getTargetDistanceMetres()));
        check("Interplanetary heliocentric radius is positive", last.getRadiusMetres() > 0.0);
    }


    private static void testLunarOrbitCapture()
    {
        ForgeOrbitTimeStepEngine engine = ForgeOrbitTimeStepEngine.lunarTransfer(200000.0);
        double transferWindow = engine.getPlannedTransferSeconds();
        double orbitPeriod = engine.getCaptureOrbitPeriodSeconds();
        advanceInDailyChunks(engine, transferWindow + 3.0 * orbitPeriod);
        ForgeOrbitTimeStepEngine.Snapshot state = engine.snapshot();
        check("Lunar mission achieves orbital capture", state.isOrbitCaptured());
        check("Lunar capture is not reported as surface impact", !state.isCollisionDetected());
        check("Lunar capture reports Moon as primary body", "Moon".equals(engine.getCapturedBodyName()));
        check("Lunar spacecraft remains above the Moon surface", state.getAltitudeMetres() > 90000.0);
        check("Lunar captured orbit is gravitationally bound", state.getSpecificEnergyJPerKg() < 0.0);
        check("Lunar insertion delta-v is recorded", Double.isFinite(engine.getCaptureDeltaVMetresPerSecond())
            && engine.getCaptureDeltaVMetresPerSecond() > 0.0);
        check("Lunar planned mission delta-v includes departure and capture",
            Double.isFinite(engine.getPlannedMissionDeltaVMetresPerSecond())
                && engine.getPlannedMissionDeltaVMetresPerSecond() > engine.getInitialDepartureDeltaVMetresPerSecond());
        double radiusBefore = state.getRadiusMetres();
        engine.step(3.0 * orbitPeriod);
        state = engine.snapshot();
        check("Lunar captured orbit remains stable for three more revolutions",
            !state.isCollisionDetected() && Math.abs(state.getRadiusMetres() - radiusBefore) < 500.0);
        check("Lunar event log confirms orbit insertion", contains(engine.getEvents(), "orbit insertion successful"));
    }

    private static void testInterplanetaryOrbitCapture()
    {
        ForgeOrbitTimeStepEngine engine = ForgeOrbitTimeStepEngine.interplanetaryTransfer(
            "Earth", "Mars", Constants.J2000_JULIAN_DATE);
        double transferWindow = engine.getPlannedTransferSeconds();
        double orbitPeriod = engine.getCaptureOrbitPeriodSeconds();
        advanceInDailyChunks(engine, transferWindow + 3.0 * orbitPeriod);
        ForgeOrbitTimeStepEngine.Snapshot state = engine.snapshot();
        check("Interplanetary mission achieves destination orbit capture", state.isOrbitCaptured());
        check("Destination capture is not reported as surface impact", !state.isCollisionDetected());
        check("Interplanetary capture reports Mars as primary body", "Mars".equals(engine.getCapturedBodyName()));
        check("Mars capture orbit is above the surface", state.getAltitudeMetres() > 150000.0);
        check("Mars captured orbit is gravitationally bound", state.getSpecificEnergyJPerKg() < 0.0);
        check("Interplanetary insertion delta-v is recorded", Double.isFinite(engine.getCaptureDeltaVMetresPerSecond())
            && engine.getCaptureDeltaVMetresPerSecond() > 0.0);
        double radiusBefore = state.getRadiusMetres();
        engine.step(3.0 * orbitPeriod);
        state = engine.snapshot();
        check("Mars captured orbit remains stable for three more revolutions",
            !state.isCollisionDetected() && Math.abs(state.getRadiusMetres() - radiusBefore) < 1000.0);
        check("Interplanetary event log confirms orbit insertion", contains(engine.getEvents(), "orbit insertion successful"));
    }

    private static void advanceInDailyChunks(ForgeOrbitTimeStepEngine engine, double seconds)
    {
        double remaining = seconds;
        while (remaining > 1.0e-6)
        {
            double chunk = Math.min(Constants.SECONDS_PER_DAY, remaining);
            engine.step(chunk);
            remaining -= chunk;
            if (engine.isCollisionDetected()) break;
        }
    }

    private static boolean contains(java.util.List<String> values, String fragment)
    {
        for (String value : values)
            if (value.toLowerCase(java.util.Locale.ROOT).contains(fragment.toLowerCase(java.util.Locale.ROOT))) return true;
        return false;
    }

    private static void check(String name, boolean result)
    {
        if (result)
        {
            passed++;
            System.out.println("PASS: " + name);
        }
        else
        {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }
}
