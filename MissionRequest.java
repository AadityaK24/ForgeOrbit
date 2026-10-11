import java.time.LocalDateTime;

public class MissionRequest
{
    private String missionName = "ForgeOrbit Mission";
    private MissionType missionType = MissionType.SATELLITE;
    private LocalDateTime launchDateTime = LocalDateTime.of(2000, 1, 1, 12, 0);
    private double initialEarthOrbitAltitudeKm = 400.0;
    private double targetEarthOrbitAltitudeKm = 800.0;
    private double targetLunarOrbitAltitudeKm = 100.0;
    private String originPlanet = "Earth";
    private String destinationPlanet = "Mars";
    private double dryMassKg = 400.0;
    private double propellantMassKg = 100.0;
    private double specificImpulseSeconds = 320.0;
    private double thrustNewtons = 5000.0;
    private double propulsionEfficiency = 0.95;
    private boolean simplifiedRequest;

    public MissionRequest() { }

    public MissionRequest(MissionType type)
    {
        setMissionType(type);
    }

    // V5 compact request constructor. Altitudes are expressed in metres.
    public MissionRequest(String name, MissionType type, double launchAltitudeMetres,
                          double targetAltitudeMetres, String originBodyName,
                          String destinationBodyName)
    {
        setMissionName(name);
        setMissionType(type);
        setInitialEarthOrbitAltitudeKm(nonNegative(launchAltitudeMetres / 1000.0, "Launch altitude"));
        setTargetEarthOrbitAltitudeKm(nonNegative(targetAltitudeMetres / 1000.0, "Target altitude"));
        setTargetLunarOrbitAltitudeKm(targetAltitudeMetres > 0.0 ? targetAltitudeMetres / 1000.0 : 100.0);
        if (originBodyName != null && !originBodyName.trim().isEmpty()) setOriginPlanet(originBodyName);
        if (destinationBodyName != null && !destinationBodyName.trim().isEmpty()) setDestinationPlanet(destinationBodyName);
        simplifiedRequest = true;
    }

    public static MissionRequest satellite(double initialAltitudeKm, double targetAltitudeKm)
    {
        MissionRequest r = new MissionRequest(MissionType.SATELLITE);
        r.setInitialEarthOrbitAltitudeKm(initialAltitudeKm);
        r.setTargetEarthOrbitAltitudeKm(targetAltitudeKm);
        return r;
    }

    public static MissionRequest lunar(double earthOrbitAltitudeKm, double lunarOrbitAltitudeKm)
    {
        MissionRequest r = new MissionRequest(MissionType.LUNAR);
        r.setInitialEarthOrbitAltitudeKm(earthOrbitAltitudeKm);
        r.setTargetLunarOrbitAltitudeKm(lunarOrbitAltitudeKm);
        return r;
    }

    public static MissionRequest interplanetary(String origin, String destination, LocalDateTime date)
    {
        MissionRequest r = new MissionRequest(MissionType.INTERPLANETARY);
        r.setOriginPlanet(origin);
        r.setDestinationPlanet(destination);
        r.setLaunchDateTime(date);
        return r;
    }

    public String getMissionName() { return missionName; }
    public void setMissionName(String value) { InputValidator.requireNonEmpty(value, "Mission name"); missionName = value.trim(); }
    public MissionType getMissionType() { return missionType; }
    public void setMissionType(MissionType value) { if (value == null) throw new IllegalArgumentException("Mission type cannot be null."); missionType = value; }
    public LocalDateTime getLaunchDateTime() { return launchDateTime; }
    public void setLaunchDateTime(LocalDateTime value) { if (value == null) throw new IllegalArgumentException("Launch date cannot be null."); launchDateTime = value; }
    public double getInitialEarthOrbitAltitudeKm() { return initialEarthOrbitAltitudeKm; }
    public void setInitialEarthOrbitAltitudeKm(double value) { initialEarthOrbitAltitudeKm = nonNegative(value, "Initial Earth orbit altitude"); }
    public double getTargetEarthOrbitAltitudeKm() { return targetEarthOrbitAltitudeKm; }
    public void setTargetEarthOrbitAltitudeKm(double value) { targetEarthOrbitAltitudeKm = nonNegative(value, "Target Earth orbit altitude"); }
    public double getTargetLunarOrbitAltitudeKm() { return targetLunarOrbitAltitudeKm; }
    public void setTargetLunarOrbitAltitudeKm(double value) { targetLunarOrbitAltitudeKm = nonNegative(value, "Target lunar orbit altitude"); }
    public String getOriginPlanet() { return originPlanet; }
    public void setOriginPlanet(String value) { InputValidator.requireNonEmpty(value, "Origin planet"); originPlanet = value.trim(); }
    public String getDestinationPlanet() { return destinationPlanet; }
    public void setDestinationPlanet(String value) { InputValidator.requireNonEmpty(value, "Destination planet"); destinationPlanet = value.trim(); }
    public double getDryMassKg() { return dryMassKg; }
    public void setDryMassKg(double value) { dryMassKg = positive(value, "Dry mass"); }
    public double getPropellantMassKg() { return propellantMassKg; }
    public void setPropellantMassKg(double value) { propellantMassKg = nonNegative(value, "Propellant mass"); }
    public double getSpecificImpulseSeconds() { return specificImpulseSeconds; }
    public void setSpecificImpulseSeconds(double value) { specificImpulseSeconds = positive(value, "Specific impulse"); }
    public double getThrustNewtons() { return thrustNewtons; }
    public void setThrustNewtons(double value) { thrustNewtons = positive(value, "Thrust"); }
    public double getPropulsionEfficiency() { return propulsionEfficiency; }
    public void setPropulsionEfficiency(double value)
    {
        if (!Double.isFinite(value) || value <= 0.0 || value > 1.0) throw new IllegalArgumentException("Propulsion efficiency must be in (0, 1].");
        propulsionEfficiency = value;
    }

    // V5 accessor aliases.
    public double getLaunchAltitudeMetres() { return initialEarthOrbitAltitudeKm * Constants.KM_TO_M; }
    public double getTargetAltitudeMetres() { return targetEarthOrbitAltitudeKm * Constants.KM_TO_M; }
    public String getOriginBodyName() { return originPlanet; }
    public String getDestinationBodyName() { return destinationPlanet; }
    public boolean isSimplifiedRequest() { return simplifiedRequest; }

    private static double positive(double value, String label)
    {
        if (!Double.isFinite(value) || value <= 0.0) throw new IllegalArgumentException(label + " must be finite and positive.");
        return value;
    }

    private static double nonNegative(double value, String label)
    {
        if (!Double.isFinite(value) || value < 0.0) throw new IllegalArgumentException(label + " must be finite and non-negative.");
        return value;
    }
}
