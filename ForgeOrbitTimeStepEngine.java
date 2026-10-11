import java.util.ArrayList;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Time-stepped spacecraft propagator for ForgeOrbit.
 *
 * Integration method: classical fourth-order Runge-Kutta (RK4).
 * Units: SI (metres, seconds, m/s, m/s^2); planetary dates are Julian dates.
 *
 * MODELS USED:
 * - Earth orbit and Hohmann transfer: Earth-centred two-body gravity.
 * - Lunar transfer: Earth-centred motion with Earth gravity and a circular,
 *   moving Moon model (including the indirect acceleration term).
 * - Interplanetary transfer: heliocentric two-body solar gravity, while target
 *   planet positions are updated from the project's approximate Keplerian
 *   PlanetaryEphemeris. Planetary perturbations, launch-vehicle ascent, finite
 *   burns, high-precision lunar ephemerides and relativistic effects are not
 *   included. This is a genuine time integration of the stated model, not a
 *   live display of a precomputed transfer-time estimate.
 *
 * Threading: call step() from the simulation/update loop; do not call the same
 * instance concurrently from the JavaFX thread and a background thread.
 */
public final class ForgeOrbitTimeStepEngine
{
    public enum Mode
    {
        EARTH_ORBIT,
        HOHMANN_TRANSFER,
        LUNAR_TRANSFER,
        INTERPLANETARY_TRANSFER
    }

    private static final double EARTH_MU = Constants.EARTH_MU;
    private static final double EARTH_RADIUS = Constants.EARTH_RADIUS;
    private static final double MOON_MU = Constants.MOON_MU;
    private static final double MOON_RADIUS = Constants.MOON_RADIUS;
    private static final double MOON_DISTANCE = Constants.EARTH_MOON_MEAN_DISTANCE;
    private static final double MOON_MEAN_MOTION = Math.sqrt(
        (Constants.EARTH_MU + Constants.MOON_MU) / (MOON_DISTANCE * MOON_DISTANCE * MOON_DISTANCE));
    private static final int MAX_PATH_POINTS = 12000;
    private static final int PLANNED_PATH_POINTS = 361;

    private final Mode mode;
    private final PlanetaryEphemeris ephemeris;
    private final String originName;
    private final String destinationName;
    private final double initialJulianDate;
    private final double centralMu;
    private final double centralRadius;
    private final double targetRadiusMetres;
    private final double moonInitialPhaseRadians;
    private final List<Vector3> actualPath;
    private final List<Vector3> plannedPath;
    private final List<String> events;

    private Vector3 position;
    private Vector3 velocity;
    private double elapsedSeconds;
    private long stepCount;
    private boolean transferComplete;
    private boolean running = true;
    private boolean collisionDetected;
    private boolean orbitCaptured;
    private String capturedBodyName;
    private double activePrimaryMu;
    private double activePrimaryRadius;
    private double targetBodyMu = Double.NaN;
    private double targetBodyRadius = Double.NaN;
    private double captureOrbitRadiusMetres = Double.NaN;
    private double captureOrbitPeriodSeconds = Double.NaN;
    private double captureDeltaVMetresPerSecond = Double.NaN;
    private double plannedCaptureDeltaVMetresPerSecond = Double.NaN;
    private double plannedTransferSeconds = Double.NaN;
    private double initialDepartureDeltaVMetresPerSecond;
    private double totalDeltaVApplied;
    private double lastAppliedDeltaV;
    private double minimumTargetDistance = Double.POSITIVE_INFINITY;
    private boolean targetApproachEventLogged;
    private double integrationStepSeconds;

    private ForgeOrbitTimeStepEngine(Mode mode, Vector3 initialPosition,
                                     Vector3 initialVelocity, double initialJulianDate,
                                     String originName, String destinationName,
                                     double centralMu, double centralRadius,
                                     double targetRadiusMetres, double moonInitialPhaseRadians,
                                     List<Vector3> plannedPath)
    {
        if (mode == null || initialPosition == null || initialVelocity == null)
            throw new IllegalArgumentException("Mode and initial state are required.");
        if (!Double.isFinite(initialJulianDate) || !Double.isFinite(centralMu) || centralMu <= 0.0)
            throw new IllegalArgumentException("Initial date and gravitational parameter must be valid.");
        if (!Double.isFinite(centralRadius) || centralRadius <= 0.0)
            throw new IllegalArgumentException("Central body radius must be positive.");
        this.mode = mode;
        this.position = initialPosition;
        this.velocity = initialVelocity;
        this.initialJulianDate = initialJulianDate;
        this.originName = originName;
        this.destinationName = destinationName;
        this.centralMu = centralMu;
        this.centralRadius = centralRadius;
        this.activePrimaryMu = centralMu;
        this.activePrimaryRadius = centralRadius;
        this.targetRadiusMetres = targetRadiusMetres;
        this.moonInitialPhaseRadians = moonInitialPhaseRadians;
        this.ephemeris = new PlanetaryEphemeris();
        this.actualPath = new ArrayList<Vector3>();
        this.plannedPath = new ArrayList<Vector3>(plannedPath);
        this.events = new ArrayList<String>();
        this.integrationStepSeconds = defaultStepFor(mode);
        this.actualPath.add(initialPosition);
        this.events.add("Simulation initialized: " + mode);
    }

    /** Start in a circular Earth orbit. altitudeMetres is above Earth's mean radius. */
    public static ForgeOrbitTimeStepEngine earthOrbit(double altitudeMetres)
    {
        return earthOrbit(altitudeMetres, 0.0);
    }

    /** Start in a circular Earth orbit at the given in-plane true anomaly. */
    public static ForgeOrbitTimeStepEngine earthOrbit(double altitudeMetres, double angleRadians)
    {
        requirePositive(altitudeMetres, "Orbit altitude");
        requireFinite(angleRadians, "Orbit angle");
        double radius = EARTH_RADIUS + altitudeMetres;
        Vector3 radial = new Vector3(Math.cos(angleRadians), Math.sin(angleRadians), 0.0);
        Vector3 tangent = new Vector3(-Math.sin(angleRadians), Math.cos(angleRadians), 0.0);
        Vector3 p = radial.scale(radius);
        Vector3 v = tangent.scale(Math.sqrt(EARTH_MU / radius));
        List<Vector3> plan = circlePath(new Vector3(0.0, 0.0, 0.0), radius, PLANNED_PATH_POINTS);
        return new ForgeOrbitTimeStepEngine(Mode.EARTH_ORBIT, p, v,
            Constants.J2000_JULIAN_DATE, "Earth", null, EARTH_MU, EARTH_RADIUS,
            Double.NaN, 0.0, plan);
    }

    /**
     * Begin an ideal Hohmann coast immediately after the first impulsive burn.
     * The engine integrates motion and performs an arrival circularization burn
     * when the target-orbit radius is crossed.
     */
    public static ForgeOrbitTimeStepEngine hohmannTransfer(double initialAltitudeMetres,
                                                            double targetAltitudeMetres)
    {
        requirePositive(initialAltitudeMetres, "Initial orbit altitude");
        requirePositive(targetAltitudeMetres, "Target orbit altitude");
        double r1 = EARTH_RADIUS + initialAltitudeMetres;
        double r2 = EARTH_RADIUS + targetAltitudeMetres;
        if (Math.abs(r1 - r2) < 1.0e-6)
            throw new IllegalArgumentException("Initial and target orbit altitudes must differ.");
        double a = (r1 + r2) / 2.0;
        double transferSpeed = Math.sqrt(EARTH_MU * (2.0 / r1 - 1.0 / a));
        Vector3 p = new Vector3(r1, 0.0, 0.0);
        Vector3 v = new Vector3(0.0, transferSpeed, 0.0);
        List<Vector3> plan = ellipseTransferPath(new Vector3(0.0, 0.0, 0.0), new Vector3(1.0, 0.0, 0.0),
            new Vector3(0.0, 1.0, 0.0), r1, r2, EARTH_MU, PLANNED_PATH_POINTS);
        ForgeOrbitTimeStepEngine engine = new ForgeOrbitTimeStepEngine(Mode.HOHMANN_TRANSFER,
            p, v, Constants.J2000_JULIAN_DATE, "Earth", null, EARTH_MU,
            EARTH_RADIUS, r2, 0.0, plan);
        engine.lastAppliedDeltaV = Math.abs(transferSpeed - Math.sqrt(EARTH_MU / r1));
        engine.initialDepartureDeltaVMetresPerSecond = engine.lastAppliedDeltaV;
        engine.totalDeltaVApplied = engine.lastAppliedDeltaV;
        engine.plannedTransferSeconds = Math.PI * Math.sqrt(a * a * a / EARTH_MU);
        double targetTransferSpeed = Math.sqrt(EARTH_MU * (2.0 / r2 - 1.0 / a));
        engine.plannedCaptureDeltaVMetresPerSecond = Math.abs(Math.sqrt(EARTH_MU / r2) - targetTransferSpeed);
        engine.events.add("First Hohmann injection burn applied at t=0: "
            + format(engine.lastAppliedDeltaV) + " m/s.");
        return engine;
    }

    /**
     * Start an Earth-centred lunar transfer. The default Moon phase is selected
     * so a circular Moon model is near the transfer apogee at the two-body
     * transfer's nominal half-period; the actual craft and Moon then advance
     * under time stepping and Moon gravity is included in the acceleration.
     */
    public static ForgeOrbitTimeStepEngine lunarTransfer(double parkingAltitudeMetres)
    {
        requirePositive(parkingAltitudeMetres, "Parking-orbit altitude");
        double r1 = EARTH_RADIUS + parkingAltitudeMetres;
        double r2 = MOON_DISTANCE;
        double a = (r1 + r2) / 2.0;
        double transferSeconds = Math.PI * Math.sqrt(a * a * a / EARTH_MU);
        double phase = normalizeAngle(Math.PI - MOON_MEAN_MOTION * transferSeconds);
        return lunarTransfer(parkingAltitudeMetres, phase);
    }

    /** Start a lunar transfer with an explicitly chosen Moon phase at t=0. */
    public static ForgeOrbitTimeStepEngine lunarTransfer(double parkingAltitudeMetres,
                                                          double moonPhaseRadians)
    {
        requirePositive(parkingAltitudeMetres, "Parking-orbit altitude");
        requireFinite(moonPhaseRadians, "Moon phase");
        double r1 = EARTH_RADIUS + parkingAltitudeMetres;
        double r2 = MOON_DISTANCE;
        double a = (r1 + r2) / 2.0;
        double transferSeconds = Math.PI * Math.sqrt(a * a * a / EARTH_MU);
        double injectionSpeed = Math.sqrt(EARTH_MU * (2.0 / r1 - 1.0 / a));
        Vector3 p = new Vector3(r1, 0.0, 0.0);
        Vector3 v = new Vector3(0.0, injectionSpeed, 0.0);
        List<Vector3> plan = ellipseTransferPath(new Vector3(0.0, 0.0, 0.0), new Vector3(1.0, 0.0, 0.0),
            new Vector3(0.0, 1.0, 0.0), r1, r2, EARTH_MU, PLANNED_PATH_POINTS);
        ForgeOrbitTimeStepEngine engine = new ForgeOrbitTimeStepEngine(Mode.LUNAR_TRANSFER,
            p, v, Constants.J2000_JULIAN_DATE, "Earth", "Moon", EARTH_MU,
            EARTH_RADIUS, MOON_DISTANCE, normalizeAngle(moonPhaseRadians), plan);
        engine.lastAppliedDeltaV = Math.abs(injectionSpeed - Math.sqrt(EARTH_MU / r1));
        engine.initialDepartureDeltaVMetresPerSecond = engine.lastAppliedDeltaV;
        engine.totalDeltaVApplied = engine.lastAppliedDeltaV;
        engine.plannedTransferSeconds = transferSeconds;
        engine.events.add("Lunar injection state initialized with " + format(engine.lastAppliedDeltaV)
            + " m/s ideal impulsive delta-v; spacecraft is propagated step by step.");
        engine.configureTargetCapture("Moon", MOON_MU, MOON_RADIUS, MOON_RADIUS + 100000.0);
        engine.estimatePlannedCaptureBurn();
        return engine;
    }

    /**
     * Start the heliocentric coast phase for an ideal Hohmann-style interplanetary
     * transfer at a specified Julian date. No launch-window estimate is generated
     * by this method; the supplied date is the initial epoch and target geometry
     * is evaluated from PlanetaryEphemeris as simulation time advances.
     */
    public static ForgeOrbitTimeStepEngine interplanetaryTransfer(String originBodyName,
                                                                   String destinationBodyName,
                                                                   LocalDateTime departureDateTime)
    {
        if (departureDateTime == null) throw new IllegalArgumentException("Departure date/time is required.");
        LocalDateTime epoch = LocalDateTime.of(2000, 1, 1, 12, 0);
        double days = Duration.between(epoch, departureDateTime).toMillis() / 86400000.0;
        return interplanetaryTransfer(originBodyName, destinationBodyName,
            Constants.J2000_JULIAN_DATE + days);
    }

    public static ForgeOrbitTimeStepEngine interplanetaryTransfer(String originBodyName,
                                                                   String destinationBodyName,
                                                                   double departureJulianDate)
    {
        if (originBodyName == null || destinationBodyName == null
            || originBodyName.trim().isEmpty() || destinationBodyName.trim().isEmpty())
            throw new IllegalArgumentException("Origin and destination names are required.");
        if (originBodyName.equalsIgnoreCase(destinationBodyName))
            throw new IllegalArgumentException("Origin and destination must differ.");
        requireFinite(departureJulianDate, "Departure Julian date");
        PlanetaryEphemeris eph = new PlanetaryEphemeris();
        CelestialBody origin = SolarSystemCatalog.getBody(originBodyName);
        CelestialBody destination = SolarSystemCatalog.getBody(destinationBodyName);
        if (origin.isStar() || destination.isStar())
            throw new IllegalArgumentException("Choose planets as origin and destination.");
        PlanetaryState originState = eph.calculateState(origin, departureJulianDate);
        double r1 = originState.getPosition().magnitude();
        double r2 = destination.getSemiMajorAxis();
        if (r1 <= 0.0 || r2 <= 0.0)
            throw new IllegalArgumentException("Planetary ephemeris returned an invalid orbital radius.");

        // Use the Hohmann half-period as the nominal flight time, then solve a
        // Lambert boundary-value problem so the propagated coast targets the
        // destination's ephemeris position at arrival instead of aiming at an
        // uncorrelated circular orbit radius.
        double transferSemiMajorAxis = (r1 + r2) / 2.0;
        double transferSeconds = Math.PI * Math.sqrt(transferSemiMajorAxis * transferSemiMajorAxis
            * transferSemiMajorAxis / Constants.SUN_MU);
        double arrivalJulianDate = departureJulianDate + transferSeconds / Constants.SECONDS_PER_DAY;
        PlanetaryState arrivalTargetState = eph.calculateState(destination, arrivalJulianDate);
        Vector3 p = originState.getPosition();
        LambertSolution lambert = solveLambert(p, arrivalTargetState.getPosition(),
            transferSeconds, Constants.SUN_MU);
        Vector3 transferVelocity = lambert.departureVelocity;
        List<Vector3> plan = new ArrayList<Vector3>();

        ForgeOrbitTimeStepEngine engine = new ForgeOrbitTimeStepEngine(
            Mode.INTERPLANETARY_TRANSFER, p, transferVelocity, departureJulianDate,
            origin.getName(), destination.getName(), Constants.SUN_MU, Constants.SUN_RADIUS,
            r2, 0.0, plan);
        engine.lastAppliedDeltaV = transferVelocity.distanceTo(originState.getVelocity());
        engine.initialDepartureDeltaVMetresPerSecond = engine.lastAppliedDeltaV;
        engine.totalDeltaVApplied = engine.lastAppliedDeltaV;
        engine.plannedTransferSeconds = transferSeconds;
        engine.events.add("Lambert departure burn planned for " + format(engine.lastAppliedDeltaV)
            + " m/s. RK4 coast is targeted at " + destination.getName()
            + " at t=" + format(transferSeconds) + " s.");
        double captureAltitude = Math.max(200000.0, destination.getRadius() * 0.03);
        engine.configureTargetCapture(destination.getName(), destination.getMu(), destination.getRadius(),
            destination.getRadius() + captureAltitude);
        engine.plannedCaptureDeltaVMetresPerSecond = Math.hypot(
            lambert.arrivalVelocity.subtract(arrivalTargetState.getVelocity()).magnitude(),
            Math.sqrt(destination.getMu() / engine.captureOrbitRadiusMetres));
        engine.buildPlannedPath(transferSeconds);
        engine.estimatePlannedCaptureBurn();
        return engine;
    }

    /** Advance simulation time by the requested positive number of seconds. */
    public synchronized Snapshot step(double requestedSeconds)
    {
        if (!Double.isFinite(requestedSeconds) || requestedSeconds <= 0.0)
            throw new IllegalArgumentException("Step duration must be finite and positive.");
        if (!running || collisionDetected)
            return snapshot();
        double remaining = requestedSeconds;
        long localSubsteps = 0L;
        while (remaining > 1.0e-9 && running && !collisionDetected)
        {
            if (++localSubsteps > 500000L)
                throw new IllegalArgumentException("Requested advance requires too many substeps. Advance less simulation time per GUI update.");
            // Capture may switch into a much smaller target-centred orbit.
            // Read the integration step every substep so that change applies
            // immediately rather than retaining the coast's large timestep.
            double h = Math.min(integrationStepSeconds, remaining);
            advanceOneSubstep(h);
            remaining -= h;
        }
        return snapshot();
    }

    /** Apply an instantaneous delta-v impulse in the current coordinate frame. */
    public synchronized void applyDeltaV(Vector3 deltaVelocityMetresPerSecond)
    {
        if (deltaVelocityMetresPerSecond == null)
            throw new IllegalArgumentException("Delta-v cannot be null.");
        if (!running || collisionDetected)
            throw new IllegalStateException("Cannot burn when the simulation is stopped or the vehicle has impacted a body.");
        velocity = velocity.add(deltaVelocityMetresPerSecond);
        lastAppliedDeltaV = deltaVelocityMetresPerSecond.magnitude();
        totalDeltaVApplied += lastAppliedDeltaV;
        addEvent("Impulse applied: " + format(lastAppliedDeltaV) + " m/s at t=" + format(elapsedSeconds) + " s.");
    }

    /** Current numerical integrator step, distinct from elapsed simulation time. */
    public synchronized double getIntegrationStepSeconds() { return integrationStepSeconds; }

    /**
     * Set the maximum RK4 step used internally. Smaller values generally improve
     * integration accuracy at additional computational cost. Large step limits
     * are restricted by mode to avoid obviously unsafe settings.
     */
    public synchronized void setIntegrationStepSeconds(double seconds)
    {
        if (!Double.isFinite(seconds) || seconds <= 0.0 || seconds > maximumAllowedStepSeconds())
            throw new IllegalArgumentException("Integration timestep must be positive and no greater than "
                + maximumAllowedStepSeconds() + " seconds for " + mode + ".");
        integrationStepSeconds = seconds;
        addEvent("Integration timestep set to " + format(seconds) + " s.");
    }

    public synchronized void pause() { running = false; }
    public synchronized void resume() { if (!collisionDetected) running = true; }
    public synchronized void stop() { running = false; }
    public synchronized boolean isRunning() { return running; }
    public synchronized boolean isCollisionDetected() { return collisionDetected; }
    public synchronized boolean isTransferComplete() { return transferComplete; }
    public synchronized boolean isOrbitCaptured() { return orbitCaptured; }
    public synchronized String getCapturedBodyName() { return capturedBodyName; }
    public synchronized double getCaptureOrbitRadiusMetres() { return captureOrbitRadiusMetres; }
    public synchronized double getCaptureOrbitAltitudeMetres() { return Double.isFinite(captureOrbitRadiusMetres) ? captureOrbitRadiusMetres - targetBodyRadius : Double.NaN; }
    public synchronized double getCaptureOrbitPeriodSeconds() { return captureOrbitPeriodSeconds; }
    public synchronized double getCaptureDeltaVMetresPerSecond() { return captureDeltaVMetresPerSecond; }
    public Mode getMode() { return mode; }
    public String getOriginName() { return originName; }
    public String getDestinationName() { return destinationName; }
    public synchronized double getElapsedSeconds() { return elapsedSeconds; }
    public synchronized long getStepCount() { return stepCount; }
    public synchronized Vector3 getPositionMetres() { return position; }
    public synchronized Vector3 getVelocityMetresPerSecond() { return velocity; }
    public synchronized double getSpeedMetresPerSecond() { return velocity.magnitude(); }
    public synchronized double getLastAppliedDeltaVMetresPerSecond() { return lastAppliedDeltaV; }
    public synchronized double getInitialDepartureDeltaVMetresPerSecond() { return initialDepartureDeltaVMetresPerSecond; }
    public synchronized double getPlannedCaptureDeltaVMetresPerSecond() { return plannedCaptureDeltaVMetresPerSecond; }
    public synchronized double getPlannedMissionDeltaVMetresPerSecond()
    {
        return Double.isFinite(plannedCaptureDeltaVMetresPerSecond)
            ? initialDepartureDeltaVMetresPerSecond + plannedCaptureDeltaVMetresPerSecond
            : Double.NaN;
    }
    public synchronized double getTotalDeltaVAppliedMetresPerSecond() { return totalDeltaVApplied; }
    public synchronized double getPlannedTransferSeconds() { return plannedTransferSeconds; }
    public synchronized double getCapturedBodyRadiusMetres() { return orbitCaptured ? activePrimaryRadius : targetBodyRadius; }
    public double getInitialJulianDate() { return initialJulianDate; }
    public synchronized double getCurrentJulianDate() { return initialJulianDate + elapsedSeconds / Constants.SECONDS_PER_DAY; }

    /** Radius from Earth in Earth-centred modes, or heliocentric radius in interplanetary mode. */
    public synchronized double getPrimaryRadiusMetres() { return position.magnitude(); }

    /** Primary-body altitude, or NaN after a frame where altitude has no physical meaning. */
    public synchronized double getPrimaryAltitudeMetres()
    {
        if (orbitCaptured) return position.magnitude() - activePrimaryRadius;
        return mode == Mode.INTERPLANETARY_TRANSFER ? Double.NaN : position.magnitude() - centralRadius;
    }

    /** Instantaneous two-body specific energy for the selected primary (J/kg). */
    public synchronized double getPrimarySpecificEnergyJPerKg()
    {
        double r = position.magnitude();
        return 0.5 * velocity.dot(velocity) - activePrimaryMu / r;
    }

    /** Distance to Moon for lunar mode, destination planet for interplanetary mode, target radius for Hohmann. */
    public synchronized double getDistanceToTargetMetres()
    {
        if (orbitCaptured) return position.magnitude();
        if (mode == Mode.LUNAR_TRANSFER)
            return position.distanceTo(moonPositionAt(elapsedSeconds));
        if (mode == Mode.INTERPLANETARY_TRANSFER)
        {
            double jd = getCurrentJulianDate();
            Vector3 target = ephemeris.calculateState(destinationName, jd).getPosition();
            return position.distanceTo(target);
        }
        if (mode == Mode.HOHMANN_TRANSFER)
            return Math.abs(position.magnitude() - targetRadiusMetres);
        return Double.NaN;
    }

    public synchronized double getMinimumTargetDistanceMetres()
    {
        return minimumTargetDistance;
    }

    public synchronized Vector3 getTargetVelocityMetresPerSecond()
    {
        if (orbitCaptured) return new Vector3(0.0, 0.0, 0.0);
        if (mode == Mode.LUNAR_TRANSFER)
        {
            double angle = moonInitialPhaseRadians + MOON_MEAN_MOTION * elapsedSeconds;
            return new Vector3(-MOON_DISTANCE * MOON_MEAN_MOTION * Math.sin(angle),
                MOON_DISTANCE * MOON_MEAN_MOTION * Math.cos(angle), 0.0);
        }
        if (mode == Mode.INTERPLANETARY_TRANSFER)
            return ephemeris.calculateState(destinationName, getCurrentJulianDate()).getVelocity();
        return null;
    }

    public synchronized double getRelativeSpeedToTargetMetresPerSecond()
    {
        Vector3 targetVelocity = getTargetVelocityMetresPerSecond();
        return targetVelocity == null ? Double.NaN : velocity.distanceTo(targetVelocity);
    }

    /** Osculating two-body period from the current state; NaN for unbound trajectories. */
    public synchronized double getOsculatingOrbitalPeriodSeconds()
    {
        double energy = getPrimarySpecificEnergyJPerKg();
        if (!Double.isFinite(energy) || energy >= 0.0) return Double.NaN;
        double semiMajorAxis = -activePrimaryMu / (2.0 * energy);
        return Constants.TWO_PI * Math.sqrt(semiMajorAxis * semiMajorAxis * semiMajorAxis / activePrimaryMu);
    }

    public synchronized Vector3 getTargetPositionMetres()
    {
        if (orbitCaptured) return new Vector3(0.0, 0.0, 0.0);
        if (mode == Mode.LUNAR_TRANSFER) return moonPositionAt(elapsedSeconds);
        if (mode == Mode.INTERPLANETARY_TRANSFER)
            return ephemeris.calculateState(destinationName, getCurrentJulianDate()).getPosition();
        return null;
    }

    public synchronized Vector3 getAccelerationMetresPerSecondSquared()
    {
        return accelerationAt(position, elapsedSeconds);
    }

    public synchronized List<Vector3> getActualTrajectory()
    {
        return Collections.unmodifiableList(new ArrayList<Vector3>(actualPath));
    }

    public List<Vector3> getPlannedTrajectory()
    {
        return Collections.unmodifiableList(new ArrayList<Vector3>(plannedPath));
    }

    public synchronized List<String> getEvents()
    {
        return Collections.unmodifiableList(new ArrayList<String>(events));
    }

    public synchronized Snapshot snapshot()
    {
        double targetDistance = getDistanceToTargetMetres();
        if (Double.isFinite(targetDistance) && targetDistance < minimumTargetDistance)
            minimumTargetDistance = targetDistance;
        return new Snapshot(mode, elapsedSeconds, stepCount, position, velocity,
            accelerationAt(position, elapsedSeconds), position.magnitude(),
            (mode == Mode.INTERPLANETARY_TRANSFER && !orbitCaptured) ? Double.NaN : position.magnitude() - activePrimaryRadius,
            targetDistance,
            getPrimarySpecificEnergyJPerKg(), getRelativeSpeedToTargetMetresPerSecond(),
            getOsculatingOrbitalPeriodSeconds(), lastAppliedDeltaV, transferComplete,
            collisionDetected, running, getCurrentJulianDate(), orbitCaptured, capturedBodyName,
            captureDeltaVMetresPerSecond);
    }

    private void advanceOneSubstep(double h)
    {
        Vector3 oldPosition = position;
        Vector3 oldVelocity = velocity;
        double oldElapsed = elapsedSeconds;
        PhaseState trial = integrate(oldPosition, oldVelocity, oldElapsed, h);

        // Capture is processed before surface-impact checks. A successful
        // lunar/planetary encounter transitions the propagator into the target-
        // centred frame and applies an explicit circular-orbit insertion burn.
        if ((mode == Mode.LUNAR_TRANSFER || mode == Mode.INTERPLANETARY_TRANSFER)
                && !orbitCaptured && tryCaptureTargetOrbit(oldPosition, oldVelocity, oldElapsed,
                    trial.position, trial.velocity, h))
            return;

        boolean radiusCrossing = mode == Mode.HOHMANN_TRANSFER && !transferComplete
            && crossesTargetRadius(oldPosition.magnitude(), trial.position.magnitude(), targetRadiusMetres);
        boolean apsisArrival = mode == Mode.HOHMANN_TRANSFER && !transferComplete
            && crossesRadialVelocityZero(oldPosition, oldVelocity, trial.position, trial.velocity)
            && Math.abs(trial.position.magnitude() - targetRadiusMetres)
                <= Math.max(1000.0, targetRadiusMetres * 1.0e-4);
        if (radiusCrossing || apsisArrival)
        {
            double eventTime = radiusCrossing
                ? locateRadiusCrossing(oldPosition, oldVelocity, oldElapsed, h, targetRadiusMetres)
                : locateRadialVelocityZero(oldPosition, oldVelocity, oldElapsed, h);
            PhaseState atTarget = integrate(oldPosition, oldVelocity, oldElapsed, eventTime);
            double reachedRadius = atTarget.position.magnitude();
            position = atTarget.position.scale(targetRadiusMetres / reachedRadius);
            velocity = atTarget.velocity;
            elapsedSeconds += eventTime;
            stepCount++;
            double beforeBurnSpeed = velocity.magnitude();
            Vector3 radial = position.normalized();
            Vector3 transverse = velocity.subtract(radial.scale(velocity.dot(radial)));
            if (transverse.magnitude() < 1.0e-9)
                throw new ArithmeticException("Cannot determine transfer direction at the target orbit.");
            Vector3 circularVelocity = transverse.normalized().scale(Math.sqrt(EARTH_MU / position.magnitude()));
            lastAppliedDeltaV = circularVelocity.distanceTo(velocity);
            totalDeltaVApplied += lastAppliedDeltaV;
            velocity = circularVelocity;
            transferComplete = true;
            addEvent("Target orbit reached at t=" + format(elapsedSeconds) + " s; circularization burn "
                + format(lastAppliedDeltaV) + " m/s applied (pre-burn speed " + format(beforeBurnSpeed) + " m/s).");
            recordPathPoint(position);
            double remainder = h - eventTime;
            if (remainder > 1.0e-9)
            {
                PhaseState afterBurn = integrate(position, velocity, elapsedSeconds, remainder);
                position = afterBurn.position;
                velocity = afterBurn.velocity;
                elapsedSeconds += remainder;
                stepCount++;
            }
        }
        else
        {
            position = trial.position;
            velocity = trial.velocity;
            elapsedSeconds += h;
            stepCount++;
        }

        if (mode == Mode.LUNAR_TRANSFER || mode == Mode.INTERPLANETARY_TRANSFER)
        {
            double distance = getDistanceToTargetMetres();
            if (Double.isFinite(distance) && distance < minimumTargetDistance)
                minimumTargetDistance = distance;
            double approachThreshold = mode == Mode.LUNAR_TRANSFER
                ? 50000000.0 : 0.02 * Constants.AU_TO_M;
            if (Double.isFinite(distance) && distance < approachThreshold && !targetApproachEventLogged)
            {
                targetApproachEventLogged = true;
                addEvent("Target approach range reached: " + format(distance / 1000.0) + " km at t="
                    + format(elapsedSeconds) + " s.");
            }
        }

        boolean movingBodyImpact = checkMovingBodyImpact(oldPosition, oldVelocity, oldElapsed,
            position, velocity, elapsedSeconds);
        if (!movingBodyImpact) checkCollision();
        recordPathPoint(position);
    }

    /** Detect a spacecraft segment crossing a moving target even if one RK4 step skips over its body radius. */
    private boolean checkMovingBodyImpact(Vector3 oldPosition, Vector3 oldVelocity, double oldElapsed,
                                          Vector3 newPosition, Vector3 newVelocity, double newElapsed)
    {
        if (mode != Mode.LUNAR_TRANSFER && mode != Mode.INTERPLANETARY_TRANSFER) return false;
        final double bodyRadius;
        final String bodyName;
        if (mode == Mode.LUNAR_TRANSFER)
        {
            bodyRadius = MOON_RADIUS;
            bodyName = "Moon";
        }
        else
        {
            CelestialBody destination = SolarSystemCatalog.getBody(destinationName);
            bodyRadius = destination.getRadius();
            bodyName = destination.getName();
        }

        Vector3 targetStart = targetPositionAt(oldElapsed);
        Vector3 targetEnd = targetPositionAt(newElapsed);
        if (targetStart == null || targetEnd == null) return false;
        Vector3 relativeStart = oldPosition.subtract(targetStart);
        Vector3 relativeEnd = newPosition.subtract(targetEnd);
        Vector3 segment = relativeEnd.subtract(relativeStart);
        double segmentLengthSquared = segment.dot(segment);
        double fraction = segmentLengthSquared <= 1.0e-18 ? 0.0
            : -relativeStart.dot(segment) / segmentLengthSquared;
        fraction = Math.max(0.0, Math.min(1.0, fraction));
        Vector3 closestRelative = relativeStart.add(segment.scale(fraction));
        double closestDistance = closestRelative.magnitude();
        if (closestDistance > bodyRadius) return false;

        elapsedSeconds = oldElapsed + (newElapsed - oldElapsed) * fraction;
        Vector3 targetAtImpact = targetPositionAt(elapsedSeconds);
        Vector3 impactDirection;
        if (closestRelative.magnitude() > 1.0e-9) impactDirection = closestRelative.normalized();
        else if (relativeStart.magnitude() > 1.0e-9) impactDirection = relativeStart.normalized();
        else impactDirection = new Vector3(1.0, 0.0, 0.0);
        position = targetAtImpact.add(impactDirection.scale(bodyRadius));
        velocity = oldVelocity.add(newVelocity.subtract(oldVelocity).scale(fraction));
        collisionDetected = true;
        running = false;
        addEvent(bodyName + " surface impact detected at t=" + format(elapsedSeconds)
            + " s. The trajectory segment intersected the target body; simulation halted.");
        return true;
    }

    private Vector3 targetPositionAt(double elapsed)
    {
        if (mode == Mode.LUNAR_TRANSFER) return moonPositionAt(elapsed);
        if (mode == Mode.INTERPLANETARY_TRANSFER)
        {
            double jd = initialJulianDate + elapsed / Constants.SECONDS_PER_DAY;
            return ephemeris.calculateState(destinationName, jd).getPosition();
        }
        return null;
    }

    private PhaseState integrate(Vector3 p, Vector3 v, double t, double h)
    {
        Vector3 k1p = v;
        Vector3 k1v = accelerationAt(p, t);

        Vector3 p2 = p.add(k1p.scale(h / 2.0));
        Vector3 v2 = v.add(k1v.scale(h / 2.0));
        Vector3 k2p = v2;
        Vector3 k2v = accelerationAt(p2, t + h / 2.0);

        Vector3 p3 = p.add(k2p.scale(h / 2.0));
        Vector3 v3 = v.add(k2v.scale(h / 2.0));
        Vector3 k3p = v3;
        Vector3 k3v = accelerationAt(p3, t + h / 2.0);

        Vector3 p4 = p.add(k3p.scale(h));
        Vector3 v4 = v.add(k3v.scale(h));
        Vector3 k4p = v4;
        Vector3 k4v = accelerationAt(p4, t + h);

        Vector3 pNext = p.add(k1p.add(k2p.scale(2.0)).add(k3p.scale(2.0)).add(k4p).scale(h / 6.0));
        Vector3 vNext = v.add(k1v.add(k2v.scale(2.0)).add(k3v.scale(2.0)).add(k4v).scale(h / 6.0));
        if (!finite(pNext) || !finite(vNext))
            throw new ArithmeticException("Numerical integration diverged; reduce the simulation timestep or check the initial state.");
        return new PhaseState(pNext, vNext);
    }

    private Vector3 accelerationAt(Vector3 p, double elapsed)
    {
        if (orbitCaptured)
            return pointGravity(p, new Vector3(0.0, 0.0, 0.0), activePrimaryMu);
        if (mode == Mode.EARTH_ORBIT || mode == Mode.HOHMANN_TRANSFER)
            return pointGravity(p, new Vector3(0.0, 0.0, 0.0), EARTH_MU);
        if (mode == Mode.LUNAR_TRANSFER)
        {
            Vector3 moon = moonPositionAt(elapsed);
            Vector3 earthGravity = pointGravity(p, new Vector3(0.0, 0.0, 0.0), Constants.EARTH_MU);
            Vector3 directMoon = pointGravity(p, moon, MOON_MU);
            Vector3 earthAccelerationDueToMoon = pointGravity(new Vector3(0.0, 0.0, 0.0), moon, MOON_MU);
            return earthGravity.add(directMoon).subtract(earthAccelerationDueToMoon);
        }
        return pointGravity(p, new Vector3(0.0, 0.0, 0.0), Constants.SUN_MU);
    }

    private Vector3 moonPositionAt(double elapsed)
    {
        double angle = moonInitialPhaseRadians + MOON_MEAN_MOTION * elapsed;
        return new Vector3(MOON_DISTANCE * Math.cos(angle), MOON_DISTANCE * Math.sin(angle), 0.0);
    }

    private void checkCollision()
    {
        if (orbitCaptured)
        {
            if (position.magnitude() <= activePrimaryRadius)
            {
                collisionDetected = true;
                running = false;
                addEvent("Captured orbit intersected " + capturedBodyName + " at t="
                    + format(elapsedSeconds) + " s; mission ended by surface impact.");
            }
            return;
        }
        if (mode == Mode.EARTH_ORBIT || mode == Mode.HOHMANN_TRANSFER || mode == Mode.LUNAR_TRANSFER)
        {
            if (position.magnitude() <= EARTH_RADIUS)
            {
                collisionDetected = true;
                running = false;
                addEvent("Earth impact condition detected at t=" + format(elapsedSeconds) + " s.");
                return;
            }
            if (mode == Mode.LUNAR_TRANSFER)
            {
                double lunarDistance = position.distanceTo(moonPositionAt(elapsedSeconds));
                if (lunarDistance <= MOON_RADIUS)
                {
                    collisionDetected = true;
                    running = false;
                    addEvent("Lunar surface impact detected at t=" + format(elapsedSeconds)
                        + " s; distance from Moon centre=" + format(lunarDistance) + " m. Simulation halted.");
                }
            }
        }
        else if (mode == Mode.INTERPLANETARY_TRANSFER)
        {
            if (position.magnitude() <= Constants.SUN_RADIUS)
            {
                collisionDetected = true;
                running = false;
                addEvent("Solar impact condition detected at t=" + format(elapsedSeconds) + " s.");
                return;
            }
            if (destinationName != null)
            {
                CelestialBody destination = SolarSystemCatalog.getBody(destinationName);
                Vector3 destinationPosition = ephemeris.calculateState(destination, getCurrentJulianDate()).getPosition();
                double destinationDistance = position.distanceTo(destinationPosition);
                if (destinationDistance <= destination.getRadius())
                {
                    collisionDetected = true;
                    running = false;
                    addEvent("Destination-body impact detected on " + destination.getName()
                        + " at t=" + format(elapsedSeconds) + " s. Simulation halted.");
                }
            }
        }
    }

    private void configureTargetCapture(String bodyName, double bodyMu, double bodyRadius, double captureRadius)
    {
        if (bodyName == null || bodyName.trim().isEmpty() || !Double.isFinite(bodyMu) || bodyMu <= 0.0
                || !Double.isFinite(bodyRadius) || bodyRadius <= 0.0 || !Double.isFinite(captureRadius)
                || captureRadius <= bodyRadius)
            throw new IllegalArgumentException("Target capture parameters are invalid.");
        this.capturedBodyName = bodyName.trim();
        this.targetBodyMu = bodyMu;
        this.targetBodyRadius = bodyRadius;
        this.captureOrbitRadiusMetres = captureRadius;
        this.captureOrbitPeriodSeconds = Constants.TWO_PI * Math.sqrt(captureRadius * captureRadius * captureRadius / bodyMu);
    }

    private void buildPlannedPath(double durationSeconds)
    {
        if (!Double.isFinite(durationSeconds) || durationSeconds <= 0.0) return;
        plannedPath.clear();
        Vector3 p = position;
        Vector3 v = velocity;
        double t = 0.0;
        plannedPath.add(p);
        for (int sample = 1; sample <= PLANNED_PATH_POINTS - 1; sample++)
        {
            double targetTime = durationSeconds * sample / (PLANNED_PATH_POINTS - 1.0);
            while (t < targetTime - 1.0e-8)
            {
                double h = Math.min(integrationStepSeconds, targetTime - t);
                PhaseState next = integrate(p, v, t, h);
                p = next.position;
                v = next.velocity;
                t += h;
            }
            plannedPath.add(p);
        }
    }

    private boolean tryCaptureTargetOrbit(Vector3 oldPosition, Vector3 oldVelocity, double oldElapsed,
                                          Vector3 newPosition, Vector3 newVelocity, double h)
    {
        if (!Double.isFinite(captureOrbitRadiusMetres) || targetBodyMu <= 0.0) return false;
        Vector3 targetStart = targetPositionAt(oldElapsed);
        Vector3 targetEnd = targetPositionAt(oldElapsed + h);
        if (targetStart == null || targetEnd == null) return false;
        Vector3 relativeStart = oldPosition.subtract(targetStart);
        Vector3 relativeEnd = newPosition.subtract(targetEnd);
        double startDistance = relativeStart.magnitude();
        if (startDistance <= captureOrbitRadiusMetres)
        {
            // Only capture inbound crossings. This branch supports an initial
            // state already at the capture shell without silently teleporting
            // a craft that begins well inside a planet.
            if (startDistance < targetBodyRadius) return false;
            if (relativeStart.dot(oldVelocity.subtract(targetVelocityAt(oldElapsed))) >= 0.0) return false;
        }
        Vector3 segment = relativeEnd.subtract(relativeStart);
        double segmentLengthSquared = segment.dot(segment);
        double closestFraction = segmentLengthSquared <= 1.0e-18 ? 0.0
            : -relativeStart.dot(segment) / segmentLengthSquared;
        closestFraction = Math.max(0.0, Math.min(1.0, closestFraction));
        double closestDistance = relativeStart.add(segment.scale(closestFraction)).magnitude();
        if (startDistance > captureOrbitRadiusMetres && closestDistance > captureOrbitRadiusMetres) return false;

        double high = startDistance <= captureOrbitRadiusMetres ? 0.0 : h * closestFraction;
        if (high <= 1.0e-9) high = Math.min(h, Math.max(1.0e-6, h * 1.0e-6));
        if (relativeDistanceAt(oldPosition, oldVelocity, oldElapsed, high) > captureOrbitRadiusMetres)
        {
            high = h;
            if (relativeDistanceAt(oldPosition, oldVelocity, oldElapsed, high) > captureOrbitRadiusMetres)
                return false;
        }
        double low = 0.0;
        if (startDistance <= captureOrbitRadiusMetres) high = 0.0;
        else
        {
            for (int i = 0; i < 48; i++)
            {
                double mid = (low + high) / 2.0;
                if (relativeDistanceAt(oldPosition, oldVelocity, oldElapsed, mid) > captureOrbitRadiusMetres)
                    low = mid;
                else high = mid;
            }
        }
        double eventSeconds = (low + high) / 2.0;
        PhaseState atCapture = eventSeconds <= 1.0e-9
            ? new PhaseState(oldPosition, oldVelocity)
            : integrate(oldPosition, oldVelocity, oldElapsed, eventSeconds);
        double eventElapsed = oldElapsed + eventSeconds;
        Vector3 targetAtCapture = targetPositionAt(eventElapsed);
        Vector3 targetVelocity = targetVelocityAt(eventElapsed);
        Vector3 relativePosition = atCapture.position.subtract(targetAtCapture);
        if (relativePosition.magnitude() < 1.0e-6) return false;
        Vector3 radial = relativePosition.normalized();
        Vector3 relativeVelocity = atCapture.velocity.subtract(targetVelocity);
        Vector3 tangent = relativeVelocity.subtract(radial.scale(relativeVelocity.dot(radial)));
        if (tangent.magnitude() < 1.0e-6)
        {
            tangent = new Vector3(-radial.getY(), radial.getX(), 0.0);
            if (tangent.magnitude() < 1.0e-6) tangent = new Vector3(0.0, -radial.getZ(), radial.getY());
            if (tangent.magnitude() < 1.0e-6) return false;
            if (tangent.dot(relativeVelocity) < 0.0) tangent = tangent.scale(-1.0);
        }
        tangent = tangent.normalized();
        double circularSpeed = Math.sqrt(targetBodyMu / captureOrbitRadiusMetres);
        Vector3 capturedVelocity = tangent.scale(circularSpeed);
        captureDeltaVMetresPerSecond = capturedVelocity.distanceTo(relativeVelocity);
        lastAppliedDeltaV = captureDeltaVMetresPerSecond;
        totalDeltaVApplied += captureDeltaVMetresPerSecond;

        // Switch from the primary-body inertial frame to the target-centred
        // frame exactly at the capture shell; apply an insertion burn to create
        // a circular, gravitationally bound orbit above the target surface.
        elapsedSeconds = eventElapsed;
        stepCount++;
        position = radial.scale(captureOrbitRadiusMetres);
        velocity = capturedVelocity;
        activePrimaryMu = targetBodyMu;
        activePrimaryRadius = targetBodyRadius;
        // A target orbit may have a period of only a few hours. Tighten the
        // coast timestep before integrating any remaining part of this step.
        integrationStepSeconds = Math.min(integrationStepSeconds,
            Math.max(1.0, Math.min(10.0, captureOrbitPeriodSeconds / 500.0)));
        orbitCaptured = true;
        transferComplete = true;
        running = true;
        actualPath.clear();
        actualPath.add(position);
        plannedPath.clear();
        plannedPath.addAll(circlePath(new Vector3(0.0, 0.0, 0.0), captureOrbitRadiusMetres, PLANNED_PATH_POINTS));
        minimumTargetDistance = captureOrbitRadiusMetres;
        addEvent(capturedBodyName + " orbit insertion successful at t=" + format(elapsedSeconds)
            + " s. Capture altitude=" + format(captureOrbitRadiusMetres - targetBodyRadius)
            + " m; insertion burn=" + format(captureDeltaVMetresPerSecond)
            + " m/s. Spacecraft is now in a target-centred circular orbit.");
        double remaining = h - eventSeconds;
        while (remaining > 1.0e-9 && !collisionDetected)
        {
            double continuationStep = Math.min(integrationStepSeconds, remaining);
            PhaseState after = integrate(position, velocity, elapsedSeconds, continuationStep);
            position = after.position;
            velocity = after.velocity;
            elapsedSeconds += continuationStep;
            stepCount++;
            remaining -= continuationStep;
            checkCollision();
            recordPathPoint(position);
        }
        return true;
    }

    private double relativeDistanceAt(Vector3 startPosition, Vector3 startVelocity, double startElapsed, double dt)
    {
        if (dt <= 1.0e-9) return startPosition.distanceTo(targetPositionAt(startElapsed));
        PhaseState state = integrate(startPosition, startVelocity, startElapsed, dt);
        return state.position.distanceTo(targetPositionAt(startElapsed + dt));
    }

    private Vector3 targetVelocityAt(double elapsed)
    {
        if (mode == Mode.LUNAR_TRANSFER)
        {
            double angle = moonInitialPhaseRadians + MOON_MEAN_MOTION * elapsed;
            return new Vector3(-MOON_DISTANCE * MOON_MEAN_MOTION * Math.sin(angle),
                MOON_DISTANCE * MOON_MEAN_MOTION * Math.cos(angle), 0.0);
        }
        if (mode == Mode.INTERPLANETARY_TRANSFER)
        {
            double jd = initialJulianDate + elapsed / Constants.SECONDS_PER_DAY;
            return ephemeris.calculateState(destinationName, jd).getVelocity();
        }
        return new Vector3(0.0, 0.0, 0.0);
    }

    private static LambertSolution solveLambert(Vector3 r1Vector, Vector3 r2Vector,
                                                double timeOfFlight, double mu)
    {
        double r1 = r1Vector.magnitude();
        double r2 = r2Vector.magnitude();
        double cosDelta = r1Vector.dot(r2Vector) / (r1 * r2);
        cosDelta = Math.max(-1.0, Math.min(1.0, cosDelta));
        double oneMinusCos = 1.0 - cosDelta;
        if (oneMinusCos < 1.0e-10)
            throw new IllegalArgumentException("Lambert transfer geometry is nearly collinear; choose a different departure date.");
        // Short-way Lambert arc. The sign is encoded in the actual endpoint vectors.
        double A = Math.sqrt(r1 * r2 * (1.0 + cosDelta));
        if (A < 1.0e-8)
            throw new IllegalArgumentException("Lambert transfer endpoints are nearly opposite; choose a different departure date.");
        double low = 0.0;
        double high = 39.0;
        double lowTime = lambertTimeOfFlight(low, r1, r2, A, mu);
        double highTime = lambertTimeOfFlight(high, r1, r2, A, mu);
        if (!Double.isFinite(lowTime) || !Double.isFinite(highTime) || timeOfFlight < lowTime || timeOfFlight > highTime)
            throw new IllegalArgumentException("Unable to solve Lambert transfer for the selected endpoints and nominal flight time.");
        for (int i = 0; i < 100; i++)
        {
            double mid = (low + high) / 2.0;
            double midTime = lambertTimeOfFlight(mid, r1, r2, A, mu);
            if (!Double.isFinite(midTime) || midTime > timeOfFlight) high = mid;
            else low = mid;
        }
        double z = (low + high) / 2.0;
        double c = stumpffC(z);
        double ss = stumpffS(z);
        double y = r1 + r2 + A * (z * ss - 1.0) / Math.sqrt(c);
        if (y <= 0.0) throw new IllegalArgumentException("Lambert solution produced a non-positive transfer parameter.");
        double f = 1.0 - y / r1;
        double g = A * Math.sqrt(y / mu);
        double gDot = 1.0 - y / r2;
        if (Math.abs(g) < 1.0e-12) throw new IllegalArgumentException("Lambert solution is numerically singular.");
        Vector3 departureVelocity = r2Vector.subtract(r1Vector.scale(f)).scale(1.0 / g);
        Vector3 arrivalVelocity = r2Vector.scale(gDot).subtract(r1Vector).scale(1.0 / g);
        if (!finite(departureVelocity) || !finite(arrivalVelocity))
            throw new IllegalArgumentException("Lambert solution returned an invalid endpoint velocity.");
        return new LambertSolution(departureVelocity, arrivalVelocity);
    }

    private static double lambertTimeOfFlight(double z, double r1, double r2, double A, double mu)
    {
        double c = stumpffC(z);
        double ss = stumpffS(z);
        if (!Double.isFinite(c) || c <= 0.0 || !Double.isFinite(ss)) return Double.NaN;
        double y = r1 + r2 + A * (z * ss - 1.0) / Math.sqrt(c);
        if (!Double.isFinite(y) || y < 0.0) return Double.NaN;
        double x = Math.sqrt(y / c);
        return (x * x * x * ss + A * Math.sqrt(y)) / Math.sqrt(mu);
    }

    private static double stumpffC(double z)
    {
        if (z > 1.0e-7)
        {
            double root = Math.sqrt(z);
            return (1.0 - Math.cos(root)) / z;
        }
        if (z < -1.0e-7)
        {
            double root = Math.sqrt(-z);
            return (Math.cosh(root) - 1.0) / (-z);
        }
        return 0.5 - z / 24.0 + z * z / 720.0;
    }

    private static double stumpffS(double z)
    {
        if (z > 1.0e-7)
        {
            double root = Math.sqrt(z);
            return (root - Math.sin(root)) / (root * root * root);
        }
        if (z < -1.0e-7)
        {
            double root = Math.sqrt(-z);
            return (Math.sinh(root) - root) / (root * root * root);
        }
        return 1.0 / 6.0 - z / 120.0 + z * z / 5040.0;
    }

    /** Run a private copy through the planned flight time to estimate the actual capture burn. */
    private void estimatePlannedCaptureBurn()
    {
        if (!(mode == Mode.LUNAR_TRANSFER || mode == Mode.INTERPLANETARY_TRANSFER)
                || !Double.isFinite(plannedTransferSeconds) || plannedTransferSeconds <= 0.0)
            return;
        Vector3 initialPosition = actualPath.get(0);
        ForgeOrbitTimeStepEngine predictor = new ForgeOrbitTimeStepEngine(mode,
            initialPosition, velocity, initialJulianDate, originName, destinationName,
            centralMu, centralRadius, targetRadiusMetres, moonInitialPhaseRadians,
            new ArrayList<Vector3>(plannedPath));
        predictor.configureTargetCapture(capturedBodyName, targetBodyMu, targetBodyRadius, captureOrbitRadiusMetres);
        predictor.initialDepartureDeltaVMetresPerSecond = initialDepartureDeltaVMetresPerSecond;
        predictor.lastAppliedDeltaV = initialDepartureDeltaVMetresPerSecond;
        predictor.totalDeltaVApplied = initialDepartureDeltaVMetresPerSecond;
        predictor.plannedTransferSeconds = plannedTransferSeconds;
        // Long outer-planet transfers can span several decades. The private
        // preflight trajectory uses enough coarse coast steps to stay below
        // the per-call work limit, then the engine automatically refines the
        // step size after capture to resolve the target-centred orbit.
        double preflightStep = plannedTransferSeconds / 400000.0;
        if (preflightStep > predictor.integrationStepSeconds)
            predictor.integrationStepSeconds = Math.min(7200.0, preflightStep);
        predictor.step(plannedTransferSeconds);
        if (!predictor.orbitCaptured || !Double.isFinite(predictor.captureDeltaVMetresPerSecond))
            throw new IllegalArgumentException("The selected transfer did not reach the target's capture orbit. Choose a different departure date or transfer setup.");
        plannedCaptureDeltaVMetresPerSecond = predictor.captureDeltaVMetresPerSecond;
    }

    private double maximumAllowedStepSeconds()
    {
        switch (mode)
        {
            case EARTH_ORBIT: return 60.0;
            case HOHMANN_TRANSFER: return 30.0;
            case LUNAR_TRANSFER: return 120.0;
            case INTERPLANETARY_TRANSFER: return 3600.0;
            default: throw new IllegalStateException("Unsupported simulation mode.");
        }
    }

    private static double defaultStepFor(Mode mode)
    {
        switch (mode)
        {
            case EARTH_ORBIT: return 5.0;
            case HOHMANN_TRANSFER: return 5.0;
            case LUNAR_TRANSFER: return 10.0;
            // Hour-scale coast steps keep multi-year outer-planet transfers
            // computationally practical. The GUI tightens this automatically
            // on capture, when the target-centred orbit period is only hours.
            case INTERPLANETARY_TRANSFER: return 3600.0;
            default: throw new IllegalArgumentException("Unsupported simulation mode.");
        }
    }

    private double locateRadiusCrossing(Vector3 p, Vector3 v, double t, double h, double targetRadius)
    {
        double low = 0.0;
        double high = h;
        double initialDifference = p.magnitude() - targetRadius;
        for (int i = 0; i < 42; i++)
        {
            double mid = (low + high) / 2.0;
            PhaseState state = integrate(p, v, t, mid);
            double difference = state.position.magnitude() - targetRadius;
            if (difference == 0.0) return mid;
            if (Math.signum(difference) == Math.signum(initialDifference)) low = mid;
            else high = mid;
        }
        return (low + high) / 2.0;
    }


    private static boolean crossesRadialVelocityZero(Vector3 p0, Vector3 v0, Vector3 p1, Vector3 v1)
    {
        double q0 = p0.dot(v0) / p0.magnitude();
        double q1 = p1.dot(v1) / p1.magnitude();
        return q0 * q1 <= 0.0 && Math.abs(q0 - q1) > 1.0e-9;
    }

    private double locateRadialVelocityZero(Vector3 p, Vector3 v, double t, double h)
    {
        double low = 0.0;
        double high = h;
        double initialRadialSpeed = p.dot(v) / p.magnitude();
        for (int i = 0; i < 42; i++)
        {
            double mid = (low + high) / 2.0;
            PhaseState state = integrate(p, v, t, mid);
            double radialSpeed = state.position.dot(state.velocity) / state.position.magnitude();
            if (Math.abs(radialSpeed) < 1.0e-12) return mid;
            if (Math.signum(radialSpeed) == Math.signum(initialRadialSpeed)) low = mid;
            else high = mid;
        }
        return (low + high) / 2.0;
    }

    private static boolean crossesTargetRadius(double r0, double r1, double target)
    {
        return (r0 - target) * (r1 - target) <= 0.0 && Math.abs(r1 - r0) > 1.0e-6;
    }

    private void recordPathPoint(Vector3 point)
    {
        actualPath.add(point);
        if (actualPath.size() > MAX_PATH_POINTS)
        {
            List<Vector3> reduced = new ArrayList<Vector3>(actualPath.size() / 2 + 1);
            reduced.add(actualPath.get(0));
            for (int i = 2; i < actualPath.size(); i += 2) reduced.add(actualPath.get(i));
            if (reduced.get(reduced.size() - 1) != point) reduced.add(point);
            actualPath.clear();
            actualPath.addAll(reduced);
        }
    }

    private void addEvent(String event)
    {
        if (events.size() >= 200) events.remove(0);
        events.add(event);
    }

    private static Vector3 pointGravity(Vector3 position, Vector3 bodyPosition, double mu)
    {
        Vector3 displacement = bodyPosition.subtract(position);
        double radius = displacement.magnitude();
        if (radius < 1.0)
            throw new ArithmeticException("Spacecraft is too close to a gravitating body centre for this point-mass model.");
        return displacement.scale(mu / (radius * radius * radius));
    }

    private static List<Vector3> circlePath(Vector3 centre, double radius, int count)
    {
        List<Vector3> points = new ArrayList<Vector3>(count);
        for (int i = 0; i < count; i++)
        {
            double angle = Constants.TWO_PI * i / (count - 1.0);
            points.add(centre.add(new Vector3(radius * Math.cos(angle), radius * Math.sin(angle), 0.0)));
        }
        return points;
    }

    private static List<Vector3> ellipseTransferPath(Vector3 focus, Vector3 periapsisAxis,
                                                      Vector3 transverseAxis, double radius1,
                                                      double radius2, double mu, int count)
    {
        double a = (radius1 + radius2) / 2.0;
        double e = Math.abs(radius2 - radius1) / (radius1 + radius2);
        double p = a * (1.0 - e * e);
        double startNu = radius2 >= radius1 ? 0.0 : Math.PI;
        List<Vector3> points = new ArrayList<Vector3>(count);
        for (int i = 0; i < count; i++)
        {
            double nu = startNu + Math.PI * i / (count - 1.0);
            double r = p / (1.0 + e * Math.cos(nu));
            Vector3 offset = periapsisAxis.scale(r * Math.cos(nu))
                .add(transverseAxis.scale(r * Math.sin(nu)));
            points.add(focus.add(offset));
        }
        return points;
    }

    private static Vector3 cross(Vector3 a, Vector3 b)
    {
        return new Vector3(a.getY() * b.getZ() - a.getZ() * b.getY(),
            a.getZ() * b.getX() - a.getX() * b.getZ(),
            a.getX() * b.getY() - a.getY() * b.getX());
    }

    private static boolean finite(Vector3 v)
    {
        return Double.isFinite(v.getX()) && Double.isFinite(v.getY()) && Double.isFinite(v.getZ());
    }

    private static double normalizeAngle(double angle)
    {
        double result = angle % Constants.TWO_PI;
        return result < 0.0 ? result + Constants.TWO_PI : result;
    }

    private static void requirePositive(double value, String label)
    {
        if (!Double.isFinite(value) || value <= 0.0)
            throw new IllegalArgumentException(label + " must be finite and positive.");
    }

    private static void requireFinite(double value, String label)
    {
        if (!Double.isFinite(value)) throw new IllegalArgumentException(label + " must be finite.");
    }

    private static String format(double value)
    {
        return String.format(java.util.Locale.US, "%.3f", value);
    }

    private static final class LambertSolution
    {
        private final Vector3 departureVelocity;
        private final Vector3 arrivalVelocity;
        private LambertSolution(Vector3 departureVelocity, Vector3 arrivalVelocity)
        {
            this.departureVelocity = departureVelocity;
            this.arrivalVelocity = arrivalVelocity;
        }
    }

    private static final class PhaseState
    {
        private final Vector3 position;
        private final Vector3 velocity;
        private PhaseState(Vector3 position, Vector3 velocity)
        {
            this.position = position;
            this.velocity = velocity;
        }
    }

    /** Immutable snapshot intended for binding to GUI telemetry cards. */
    public static final class Snapshot
    {
        private final Mode mode;
        private final double elapsedSeconds;
        private final long stepCount;
        private final Vector3 position;
        private final Vector3 velocity;
        private final Vector3 acceleration;
        private final double radiusMetres;
        private final double altitudeMetres;
        private final double targetDistanceMetres;
        private final double specificEnergyJPerKg;
        private final double relativeSpeedToTargetMetresPerSecond;
        private final double osculatingOrbitalPeriodSeconds;
        private final double lastDeltaVMetresPerSecond;
        private final boolean transferComplete;
        private final boolean collisionDetected;
        private final boolean running;
        private final double julianDate;
        private final boolean orbitCaptured;
        private final String capturedBodyName;
        private final double captureDeltaVMetresPerSecond;

        private Snapshot(Mode mode, double elapsedSeconds, long stepCount, Vector3 position,
                         Vector3 velocity, Vector3 acceleration, double radiusMetres,
                         double altitudeMetres, double targetDistanceMetres,
                         double specificEnergyJPerKg, double relativeSpeedToTargetMetresPerSecond,
                         double osculatingOrbitalPeriodSeconds, double lastDeltaVMetresPerSecond,
                         boolean transferComplete, boolean collisionDetected,
                         boolean running, double julianDate, boolean orbitCaptured,
                         String capturedBodyName, double captureDeltaVMetresPerSecond)
        {
            this.mode = mode;
            this.elapsedSeconds = elapsedSeconds;
            this.stepCount = stepCount;
            this.position = position;
            this.velocity = velocity;
            this.acceleration = acceleration;
            this.radiusMetres = radiusMetres;
            this.altitudeMetres = altitudeMetres;
            this.targetDistanceMetres = targetDistanceMetres;
            this.specificEnergyJPerKg = specificEnergyJPerKg;
            this.relativeSpeedToTargetMetresPerSecond = relativeSpeedToTargetMetresPerSecond;
            this.osculatingOrbitalPeriodSeconds = osculatingOrbitalPeriodSeconds;
            this.lastDeltaVMetresPerSecond = lastDeltaVMetresPerSecond;
            this.transferComplete = transferComplete;
            this.collisionDetected = collisionDetected;
            this.running = running;
            this.julianDate = julianDate;
            this.orbitCaptured = orbitCaptured;
            this.capturedBodyName = capturedBodyName;
            this.captureDeltaVMetresPerSecond = captureDeltaVMetresPerSecond;
        }
        public Mode getMode() { return mode; }
        public double getElapsedSeconds() { return elapsedSeconds; }
        public long getStepCount() { return stepCount; }
        public Vector3 getPositionMetres() { return position; }
        public Vector3 getVelocityMetresPerSecond() { return velocity; }
        public Vector3 getAccelerationMetresPerSecondSquared() { return acceleration; }
        public double getSpeedMetresPerSecond() { return velocity.magnitude(); }
        public double getRadiusMetres() { return radiusMetres; }
        public double getAltitudeMetres() { return altitudeMetres; }
        public double getTargetDistanceMetres() { return targetDistanceMetres; }
        public double getSpecificEnergyJPerKg() { return specificEnergyJPerKg; }
        public double getRelativeSpeedToTargetMetresPerSecond() { return relativeSpeedToTargetMetresPerSecond; }
        public double getOsculatingOrbitalPeriodSeconds() { return osculatingOrbitalPeriodSeconds; }
        public double getLastDeltaVMetresPerSecond() { return lastDeltaVMetresPerSecond; }
        public boolean isTransferComplete() { return transferComplete; }
        public boolean isCollisionDetected() { return collisionDetected; }
        public boolean isRunning() { return running; }
        public double getJulianDate() { return julianDate; }
        public boolean isOrbitCaptured() { return orbitCaptured; }
        public String getCapturedBodyName() { return capturedBodyName; }
        public double getCaptureDeltaVMetresPerSecond() { return captureDeltaVMetresPerSecond; }
    }
}
