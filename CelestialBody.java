public class CelestialBody
{
    private final String name;
    private final double mass;
    private final double radius;
    private final double gravitationalParameter;
    private final OrbitalElements orbitalElements;
    private final String bodyType;
    private final double rotationPeriodHours;
    private final double axialTiltDegrees;
    private final String displayColor;
    private final boolean hasRings;

    public CelestialBody(String name, double mass, double radius, double gravitationalParameter)
    {
        this(name, mass, radius, gravitationalParameter, null, "BODY", 0.0, 0.0, "#A0A0A0", false);
    }

    public CelestialBody(String name, double mass, double radius, double gravitationalParameter,
                         OrbitalElements orbitalElements, String bodyType,
                         double rotationPeriodHours, double axialTiltDegrees,
                         String displayColor, boolean hasRings)
    {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Celestial body name cannot be empty.");
        requirePositiveFinite(mass, "Mass");
        requirePositiveFinite(radius, "Radius");
        requirePositiveFinite(gravitationalParameter, "Gravitational parameter");
        if (!Double.isFinite(rotationPeriodHours) || !Double.isFinite(axialTiltDegrees))
            throw new IllegalArgumentException("Rotation period and axial tilt must be finite.");
        if (bodyType == null || bodyType.trim().isEmpty())
            throw new IllegalArgumentException("Body type cannot be empty.");
        if (displayColor == null || displayColor.trim().isEmpty())
            throw new IllegalArgumentException("Display colour cannot be empty.");
        this.name = name.trim();
        this.mass = mass;
        this.radius = radius;
        this.gravitationalParameter = gravitationalParameter;
        this.orbitalElements = orbitalElements;
        this.bodyType = bodyType.trim();
        this.rotationPeriodHours = rotationPeriodHours;
        this.axialTiltDegrees = axialTiltDegrees;
        this.displayColor = displayColor;
        this.hasRings = hasRings;
    }

    public String getName() { return name; }
    public double getMass() { return mass; }
    public double getRadius() { return radius; }
    public double getMeanRadius() { return radius; }
    public double getGravitationalParameter() { return gravitationalParameter; }
    public double getMu() { return gravitationalParameter; }
    public OrbitalElements getOrbitalElements() { return orbitalElements; }
    public String getBodyType() { return bodyType; }
    public double getRotationPeriodHours() { return rotationPeriodHours; }
    public double getAxialTiltDegrees() { return axialTiltDegrees; }
    public String getDisplayColor() { return displayColor; }
    public boolean hasRings() { return hasRings; }
    public boolean isStar() { return "STAR".equalsIgnoreCase(bodyType); }

    public double getSurfaceGravity()
    {
        return gravitationalParameter / (radius * radius);
    }

    public double getEscapeVelocity()
    {
        return Math.sqrt(2.0 * gravitationalParameter / radius);
    }

    // Compatibility accessors for the earlier circular-orbit model.
    public double getSemiMajorAxis()
    {
        if (orbitalElements == null)
            return Double.NaN;
        return orbitalElements.getSemiMajorAxisAu() * Constants.AU_TO_M;
    }

    public double getMeanMotion()
    {
        double axis = getSemiMajorAxis();
        if (!Double.isFinite(axis) || axis <= 0.0)
            return 0.0;
        return Math.sqrt(Constants.SUN_MU / (axis * axis * axis));
    }

    public double getMeanLongitudeJ2000Radians()
    {
        return orbitalElements == null ? 0.0
            : Math.toRadians(orbitalElements.getMeanLongitudeDegrees());
    }

    public double getMeanDensity()
    {
        return mass / ((4.0 / 3.0) * Math.PI * radius * radius * radius);
    }

    private static void requirePositiveFinite(double value, String label)
    {
        if (!Double.isFinite(value) || value <= 0.0)
            throw new IllegalArgumentException(label + " must be finite and positive.");
    }
}
