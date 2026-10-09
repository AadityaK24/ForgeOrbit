
public class ForgeOrbitTest
{
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args)
    {
        System.out.println("========================================");
        System.out.println("       FORGEORBIT PHYSICS TEST");
        System.out.println("========================================");

        testEarth();
        testOrbit();
        testOrbitalMechanics();
        testHohmannTransfer();
        testPropulsion();
        testPropellantCalculator();
        testMissionIntegration();
        testInputValidator();

        System.out.println();
        System.out.println("========================================");
        System.out.println("           TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("Tests Passed: " + testsPassed);
        System.out.println("Tests Failed: " + testsFailed);

        if (testsFailed == 0)
        {
            System.out.println("STATUS: ALL TESTS PASSED");
        }
        else
        {
            System.out.println("STATUS: TESTS FAILED");
        }

        System.out.println("========================================");
    }

    private static void testEarth()
    {
        System.out.println("\n--- EARTH TEST ---");

        Earth earth = new Earth();

        check(
            "Earth radius",
            earth.getRadius() == Constants.EARTH_RADIUS
        );

        check(
            "Earth mass",
            earth.getMass() == Constants.EARTH_MASS
        );

        check(
            "Earth gravitational parameter",
            earth.getGravitationalParameter() == Constants.EARTH_MU
        );

        double expectedGravity =
            Constants.EARTH_MU
            / (Constants.EARTH_RADIUS * Constants.EARTH_RADIUS);

        check(
            "Surface gravity calculated from mu and radius",
            nearlyEqual(
                earth.getSurfaceGravity(),
                expectedGravity,
                1.0e-9
            )
        );

        check(
            "Earth surface gravity is physically reasonable",
            earth.getSurfaceGravity() > 9.7
                && earth.getSurfaceGravity() < 9.9
        );

        System.out.printf(
            "Surface Gravity: %.5f m/s^2%n",
            earth.getSurfaceGravity()
        );
    }

    private static void testOrbit()
    {
        System.out.println("\n--- ORBIT TEST ---");

        Orbit orbit = new Orbit(400000.0);

        double expectedRadius =
            Constants.EARTH_RADIUS + 400000.0;

        check(
            "400 km orbit altitude",
            nearlyEqual(orbit.getAltitude(), 400000.0, 0.001)
        );

        check(
            "Orbital radius includes Earth radius",
            nearlyEqual(orbit.getRadius(), expectedRadius, 0.001)
        );

        expectInvalidOrbit(
            "Reject zero altitude",
            0.0
        );

        expectInvalidOrbit(
            "Reject negative altitude",
            -100.0
        );

        expectInvalidOrbit(
            "Reject NaN altitude",
            Double.NaN
        );

        expectInvalidOrbit(
            "Reject infinite altitude",
            Double.POSITIVE_INFINITY
        );

        System.out.printf(
            "400 km Orbital Radius: %.2f km%n",
            orbit.getRadius() / 1000.0
        );
    }

    private static void testOrbitalMechanics()
    {
        System.out.println("\n--- ORBITAL MECHANICS TEST ---");

        Orbit orbit = new Orbit(400000.0);

        OrbitalMechanics mechanics =
            new OrbitalMechanics();

        double circularVelocity =
            mechanics.calculateCircularVelocity(orbit);

        double period =
            mechanics.calculateOrbitalPeriod(orbit);

        double escapeVelocity =
            mechanics.calculateEscapeVelocity(orbit);

        double gravity =
            mechanics.calculateGravitationalAcceleration(orbit);

        double energy =
            mechanics.calculateSpecificOrbitalEnergy(orbit);

        double visVivaVelocity =
            mechanics.calculateVisVivaVelocity(
                orbit,
                orbit.getRadius()
            );

        System.out.printf(
            "Circular Velocity: %.2f m/s%n",
            circularVelocity
        );

        System.out.printf(
            "Orbital Period: %.2f s (%.2f min)%n",
            period,
            period / 60.0
        );

        System.out.printf(
            "Escape Velocity: %.2f m/s%n",
            escapeVelocity
        );

        System.out.printf(
            "Gravity at Orbit: %.4f m/s^2%n",
            gravity
        );

        System.out.printf(
            "Specific Orbital Energy: %.2f J/kg%n",
            energy
        );

        check(
            "Circular velocity at 400 km",
            nearlyEqual(circularVelocity, 7672.60, 1.0)
        );

        check(
            "Orbital period at 400 km",
            nearlyEqual(period, 5544.86, 2.0)
        );

        check(
            "Escape velocity at 400 km",
            nearlyEqual(escapeVelocity, 10850.69, 2.0)
        );

        check(
            "Gravitational acceleration at 400 km",
            nearlyEqual(gravity, 8.6943, 0.02)
        );

        check(
            "Specific orbital energy is negative",
            energy < 0.0
        );

        check(
            "Vis-viva agrees with circular velocity",
            nearlyEqual(
                visVivaVelocity,
                circularVelocity,
                0.001
            )
        );
    }

    private static void testHohmannTransfer()
    {
        System.out.println("\n--- HOHMANN TRANSFER TEST ---");

        Orbit initialOrbit = new Orbit(400000.0);
        Orbit targetOrbit = new Orbit(800000.0);

        HohmannTransfer transfer =
            new HohmannTransfer();

        double semiMajorAxis =
            transfer.calculateTransferSemiMajorAxis(
                initialOrbit,
                targetOrbit
            );

        double firstBurn =
            transfer.calculateFirstBurnDeltaV(
                initialOrbit,
                targetOrbit
            );

        double secondBurn =
            transfer.calculateSecondBurnDeltaV(
                initialOrbit,
                targetOrbit
            );

        double totalDeltaV =
            transfer.calculateTotalDeltaV(
                initialOrbit,
                targetOrbit
            );

        double transferTime =
            transfer.calculateTransferTime(
                initialOrbit,
                targetOrbit
            );

        System.out.printf(
            "Transfer Semi-Major Axis: %.2f km%n",
            semiMajorAxis / 1000.0
        );

        System.out.printf(
            "First Burn: %.2f m/s%n",
            firstBurn
        );

        System.out.printf(
            "Second Burn: %.2f m/s%n",
            secondBurn
        );

        System.out.printf(
            "Total Delta-v: %.2f m/s%n",
            totalDeltaV
        );

        System.out.printf(
            "Transfer Time: %.2f s (%.2f min)%n",
            transferTime,
            transferTime / 60.0
        );

        check(
            "Transfer semi-major axis",
            nearlyEqual(semiMajorAxis, 6971000.0, 1.0)
        );

        check(
            "First burn delta-v",
            nearlyEqual(firstBurn, 109.29, 0.5)
        );

        check(
            "Second burn delta-v",
            nearlyEqual(secondBurn, 107.73, 0.5)
        );

        check(
            "Total delta-v",
            nearlyEqual(totalDeltaV, 217.02, 0.5)
        );

        check(
            "Transfer time",
            nearlyEqual(transferTime, 2896.17, 2.0)
        );

        Orbit reverseInitial = new Orbit(800000.0);
        Orbit reverseTarget = new Orbit(400000.0);

        double reverseDeltaV =
            transfer.calculateTotalDeltaV(
                reverseInitial,
                reverseTarget
            );

        check(
            "Lowering transfer produces positive delta-v",
            reverseDeltaV > 0.0
        );

        check(
            "Reversed transfer has matching total delta-v",
            nearlyEqual(reverseDeltaV, totalDeltaV, 0.01)
        );
    }

    private static void testPropulsion()
    {
        System.out.println("\n--- PROPULSION TEST ---");

        Propulsion propulsion = new Propulsion(
            "Orbital Test Engine",
            320.0,
            5000.0,
            0.95
        );

        double exhaustVelocity =
            propulsion.calculateExhaustVelocity();

        double effectiveThrust =
            propulsion.getEffectiveThrust();

        double massFlowRate =
            propulsion.calculateMassFlowRate();

        double burnTime =
            propulsion.calculateBurnTime(10.0);

        System.out.printf(
            "Exhaust Velocity: %.2f m/s%n",
            exhaustVelocity
        );

        System.out.printf(
            "Effective Thrust: %.2f N%n",
            effectiveThrust
        );

        System.out.printf(
            "Mass Flow Rate: %.6f kg/s%n",
            massFlowRate
        );

        System.out.printf(
            "Burn Time for 10 kg: %.2f s%n",
            burnTime
        );

        check(
            "Exhaust velocity from specific impulse",
            nearlyEqual(
                exhaustVelocity,
                320.0 * Constants.STANDARD_GRAVITY,
                0.001
            )
        );

        check(
            "Effective thrust includes efficiency",
            nearlyEqual(effectiveThrust, 4750.0, 0.001)
        );

        check(
            "Mass flow rate",
            nearlyEqual(
                massFlowRate,
                effectiveThrust / exhaustVelocity,
                1.0e-9
            )
        );

        check(
            "Burn time",
            nearlyEqual(burnTime, 10.0 / massFlowRate, 0.001)
        );

        check(
            "Zero propellant produces zero burn time",
            propulsion.calculateBurnTime(0.0) == 0.0
        );
    }

    private static void testPropellantCalculator()
    {
        System.out.println("\n--- PROPELLANT CALCULATOR TEST ---");

        Propulsion propulsion = new Propulsion(
            "Orbital Test Engine",
            320.0,
            5000.0,
            0.95
        );

        PropellantCalculator calculator =
            new PropellantCalculator();

        double deltaV = 217.02;
        double dryMass = 400.0;
        double propellantMass = 100.0;

        double requiredPropellant =
            calculator.calculateRequiredPropellant(
                deltaV,
                dryMass,
                propulsion
            );

        double finalMass =
            calculator.calculateFinalMass(
                500.0,
                deltaV,
                propulsion
            );

        double availableDeltaV =
            calculator.calculateAvailableDeltaV(
                dryMass,
                propellantMass,
                propulsion
            );

        double propellantFraction =
            calculator.calculatePropellantFraction(
                dryMass,
                propellantMass
            );

        double recoveredDeltaV =
            calculator.calculateAvailableDeltaV(
                dryMass,
                requiredPropellant,
                propulsion
            );

        System.out.printf(
            "Required Propellant: %.4f kg%n",
            requiredPropellant
        );

        System.out.printf(
            "Final Mass after Manoeuvre: %.4f kg%n",
            finalMass
        );

        System.out.printf(
            "Available Delta-v: %.2f m/s%n",
            availableDeltaV
        );

        System.out.printf(
            "Propellant Fraction: %.4f%n",
            propellantFraction
        );

        check(
            "Required propellant",
            nearlyEqual(requiredPropellant, 28.64, 0.15)
        );

        check(
            "Final mass is below initial mass",
            finalMass > 400.0 && finalMass < 500.0
        );

        check(
            "Final mass matches rocket equation",
            nearlyEqual(
                finalMass,
                calculator.calculateFinalMass(
                    500.0,
                    deltaV,
                    propulsion
                ),
                0.001
            )
        );

        check(
            "Available delta-v",
            nearlyEqual(availableDeltaV, 700.25, 1.0)
        );

        check(
            "Propellant fraction",
            nearlyEqual(propellantFraction, 0.20, 0.001)
        );

        check(
            "Rocket equation reverses required propellant",
            nearlyEqual(recoveredDeltaV, deltaV, 0.001)
        );

        check(
            "Zero delta-v requires zero propellant",
            calculator.calculateRequiredPropellant(
                0.0,
                dryMass,
                propulsion
            ) == 0.0
        );

        check(
            "Zero propellant gives zero available delta-v",
            calculator.calculateAvailableDeltaV(
                dryMass,
                0.0,
                propulsion
            ) == 0.0
        );
    }

    private static void testMissionIntegration()
    {
        System.out.println("\n--- MISSION INTEGRATION TEST ---");

        Orbit initialOrbit = new Orbit(400000.0);
        Orbit targetOrbit = new Orbit(800000.0);

        Satellite satellite = new Satellite(
            "ForgeSat-1",
            400.0,
            100.0,
            initialOrbit,
            targetOrbit
        );

        Propulsion propulsion = new Propulsion(
            "Orbital Test Engine",
            320.0,
            5000.0,
            0.95
        );

        Mission mission = new Mission(
            "LEO Raising Mission",
            satellite,
            propulsion
        );

        TransferResult result =
            mission.calculateTransfer();

        MissionAnalysis analysis =
            new MissionAnalysis();

        check(
            "Mission name",
            mission.getMissionName().equals(
                "LEO Raising Mission"
            )
        );

        check(
            "Mission satellite association",
            mission.getSatellite() == satellite
        );

        check(
            "Mission propulsion association",
            mission.getPropulsion() == propulsion
        );

        check(
            "Transfer result exists",
            result != null
        );

        if (result == null)
        {
            System.out.println(
                "[FAIL] Remaining mission tests skipped: no transfer result."
            );

            testsFailed++;

            return;
        }

        double requiredPropellant =
            analysis.calculateRequiredPropellant(
                mission,
                result
            );

        double availableDeltaV =
            analysis.calculateAvailableDeltaV(mission);

        double margin =
            analysis.calculateDeltaVMargin(
                mission,
                result
            );

        boolean feasible =
            analysis.isMissionFeasible(
                mission,
                result
            );

        String verdict =
            analysis.generateVerdict(
                mission,
                result
            );

        System.out.printf(
            "Mission Delta-v: %.2f m/s%n",
            result.getTotalDeltaV()
        );

        System.out.printf(
            "Required Propellant: %.2f kg%n",
            requiredPropellant
        );

        System.out.printf(
            "Available Delta-v: %.2f m/s%n",
            availableDeltaV
        );

        System.out.printf(
            "Delta-v Margin: %.2f m/s%n",
            margin
        );

        System.out.println("Feasible: " + feasible);
        System.out.println("Verdict: " + verdict);

        check(
            "Mission delta-v matches transfer calculation",
            nearlyEqual(result.getTotalDeltaV(), 217.02, 0.5)
        );

        check(
            "Mission required propellant",
            nearlyEqual(requiredPropellant, 28.64, 0.15)
        );

        check(
            "Available delta-v exceeds required delta-v",
            availableDeltaV > result.getTotalDeltaV()
        );

        check(
            "Feasible mission has positive margin",
            margin > 0.0
        );

        check(
            "Mission feasibility is true",
            feasible
        );

        check(
            "Mission verdict is feasible",
            verdict.equals("MISSION FEASIBLE")
        );

        // Repeat the mission with insufficient propellant.
        Satellite lowFuelSatellite = new Satellite(
            "ForgeSat-LowFuel",
            400.0,
            1.0,
            new Orbit(400000.0),
            new Orbit(800000.0)
        );

        Mission lowFuelMission = new Mission(
            "Low Fuel Test",
            lowFuelSatellite,
            propulsion
        );

        TransferResult lowFuelResult =
            lowFuelMission.calculateTransfer();

        boolean lowFuelFeasible =
            analysis.isMissionFeasible(
                lowFuelMission,
                lowFuelResult
            );

        String lowFuelVerdict =
            analysis.generateVerdict(
                lowFuelMission,
                lowFuelResult
            );

        check(
            "Insufficient propellant makes mission infeasible",
            !lowFuelFeasible
        );

        check(
            "Insufficient-propellant verdict",
            lowFuelVerdict.equals("MISSION NOT FEASIBLE")
        );
    }

    private static void testInputValidator()
    {
        System.out.println("\n--- INPUT VALIDATOR TEST ---");

        InputValidator validator =
            new InputValidator();

        check(
            "Valid mission name",
            validator.isValidMissionName("ForgeSat-1")
        );

        check(
            "Reject empty mission name",
            !validator.isValidMissionName("")
        );

        check(
            "Reject whitespace-only name",
            !validator.isValidMissionName("   ")
        );

        check(
            "Valid altitude",
            validator.isValidAltitude(400000.0)
        );

        check(
            "Reject zero altitude",
            !validator.isValidAltitude(0.0)
        );

        check(
            "Reject NaN altitude",
            !validator.isValidAltitude(Double.NaN)
        );

        check(
            "Valid dry mass",
            validator.isValidMass(400.0)
        );

        check(
            "Reject negative mass",
            !validator.isValidMass(-1.0)
        );

        check(
            "Zero propellant is allowed",
            validator.isValidPropellantMass(0.0)
        );

        check(
            "Reject negative propellant",
            !validator.isValidPropellantMass(-1.0)
        );

        check(
            "Valid specific impulse",
            validator.isValidSpecificImpulse(320.0)
        );

        check(
            "Valid thrust",
            validator.isValidThrust(5000.0)
        );

        check(
            "Valid efficiency",
            validator.isValidEfficiency(0.95)
        );

        check(
            "Efficiency of one is allowed",
            validator.isValidEfficiency(1.0)
        );

        check(
            "Reject efficiency above one",
            !validator.isValidEfficiency(1.1)
        );

        check(
            "Valid orbit pair",
            validator.areValidOrbitAltitudes(
                400000.0,
                800000.0
            )
        );

        check(
            "Reject identical orbit altitudes",
            !validator.areValidOrbitAltitudes(
                400000.0,
                400000.0
            )
        );

        check(
            "Valid satellite mass data",
            validator.isValidSatelliteData(
                400.0,
                100.0
            )
        );

        check(
            "Valid propulsion data",
            validator.isValidPropulsionData(
                320.0,
                5000.0,
                0.95
            )
        );
    }

    private static void expectInvalidOrbit(
        String testName,
        double altitude)
    {
        try
        {
            new Orbit(altitude);

            check(testName, false);
        }
        catch (IllegalArgumentException exception)
        {
            check(testName, true);
        }
    }

    private static void check(
        String testName,
        boolean condition)
    {
        if (condition)
        {
            System.out.println("[PASS] " + testName);
            testsPassed++;
        }
        else
        {
            System.out.println("[FAIL] " + testName);
            testsFailed++;
        }
    }

    private static boolean nearlyEqual(
        double actual,
        double expected,
        double tolerance)
    {
        return Double.isFinite(actual)
            && Math.abs(actual - expected) <= tolerance;
    }
}
