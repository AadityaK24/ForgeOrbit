/**
 * Approximate JPL-style Keplerian elements. Distances are in AU, angles in degrees,
 * and rates are per Julian century from the J2000.0 epoch.
 */
public final class OrbitalElements
{
    private final double semiMajorAxisAu;
    private final double semiMajorAxisRate;
    private final double eccentricity;
    private final double eccentricityRate;
    private final double inclinationDegrees;
    private final double inclinationRate;
    private final double meanLongitudeDegrees;
    private final double meanLongitudeRate;
    private final double longitudePerihelionDegrees;
    private final double longitudePerihelionRate;
    private final double ascendingNodeDegrees;
    private final double ascendingNodeRate;
    private final double b;
    private final double c;
    private final double s;
    private final double f;

    public OrbitalElements(double semiMajorAxisAu, double semiMajorAxisRate,
                           double eccentricity, double eccentricityRate,
                           double inclinationDegrees, double inclinationRate,
                           double meanLongitudeDegrees, double meanLongitudeRate,
                           double longitudePerihelionDegrees, double longitudePerihelionRate,
                           double ascendingNodeDegrees, double ascendingNodeRate,
                           double b, double c, double s, double f)
    {
        if (!Double.isFinite(semiMajorAxisAu) || semiMajorAxisAu <= 0.0)
            throw new IllegalArgumentException("Semimajor axis must be positive and finite.");
        if (!Double.isFinite(eccentricity) || eccentricity < 0.0 || eccentricity >= 1.0)
            throw new IllegalArgumentException("Eccentricity must be in the range [0, 1).");
        double[] values = {semiMajorAxisRate, eccentricityRate, inclinationDegrees,
            inclinationRate, meanLongitudeDegrees, meanLongitudeRate,
            longitudePerihelionDegrees, longitudePerihelionRate,
            ascendingNodeDegrees, ascendingNodeRate, b, c, s, f};
        for (double value : values)
        {
            if (!Double.isFinite(value))
                throw new IllegalArgumentException("Orbital elements must be finite.");
        }
        this.semiMajorAxisAu = semiMajorAxisAu;
        this.semiMajorAxisRate = semiMajorAxisRate;
        this.eccentricity = eccentricity;
        this.eccentricityRate = eccentricityRate;
        this.inclinationDegrees = inclinationDegrees;
        this.inclinationRate = inclinationRate;
        this.meanLongitudeDegrees = meanLongitudeDegrees;
        this.meanLongitudeRate = meanLongitudeRate;
        this.longitudePerihelionDegrees = longitudePerihelionDegrees;
        this.longitudePerihelionRate = longitudePerihelionRate;
        this.ascendingNodeDegrees = ascendingNodeDegrees;
        this.ascendingNodeRate = ascendingNodeRate;
        this.b = b;
        this.c = c;
        this.s = s;
        this.f = f;
    }

    public double getSemiMajorAxisAu() { return semiMajorAxisAu; }
    public double getSemiMajorAxisRate() { return semiMajorAxisRate; }
    public double getEccentricity() { return eccentricity; }
    public double getEccentricityRate() { return eccentricityRate; }
    public double getInclinationDegrees() { return inclinationDegrees; }
    public double getInclinationRate() { return inclinationRate; }
    public double getMeanLongitudeDegrees() { return meanLongitudeDegrees; }
    public double getMeanLongitudeRate() { return meanLongitudeRate; }
    public double getLongitudePerihelionDegrees() { return longitudePerihelionDegrees; }
    public double getLongitudePerihelionRate() { return longitudePerihelionRate; }
    public double getAscendingNodeDegrees() { return ascendingNodeDegrees; }
    public double getAscendingNodeRate() { return ascendingNodeRate; }
    public double getB() { return b; }
    public double getC() { return c; }
    public double getS() { return s; }
    public double getF() { return f; }

    public double semiMajorAxisAuAt(double centuries)
    {
        return semiMajorAxisAu + semiMajorAxisRate * centuries;
    }

    public double eccentricityAt(double centuries)
    {
        return eccentricity + eccentricityRate * centuries;
    }

    public double inclinationDegreesAt(double centuries)
    {
        return inclinationDegrees + inclinationRate * centuries;
    }

    public double meanLongitudeDegreesAt(double centuries)
    {
        return meanLongitudeDegrees + meanLongitudeRate * centuries;
    }

    public double longitudePerihelionDegreesAt(double centuries)
    {
        return longitudePerihelionDegrees + longitudePerihelionRate * centuries;
    }

    public double ascendingNodeDegreesAt(double centuries)
    {
        return ascendingNodeDegrees + ascendingNodeRate * centuries;
    }
}
