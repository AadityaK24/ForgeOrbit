public class Satellite
{
    private final String name;
    private final double dryMass;
    private double propellantMass;
    private Orbit currentOrbit;
    private Orbit targetOrbit;

    public Satellite(
        String name,
        double dryMass,
        double propellantMass,
        Orbit currentOrbit,
        Orbit targetOrbit)
    {
        if (name == null || name.trim().isEmpty())
        {
            throw new IllegalArgumentException(
                "Satellite name cannot be empty."
            );
        }

        if (dryMass <= 0)
        {
            throw new IllegalArgumentException(
                "Dry mass must be greater than 0."
            );
        }

        if (propellantMass < 0)
        {
            throw new IllegalArgumentException(
                "Propellant mass cannot be negative."
            );
        }

        if (currentOrbit == null || targetOrbit == null)
        {
            throw new IllegalArgumentException(
                "Orbits cannot be null."
            );
        }

        this.name = name;
        this.dryMass = dryMass;
        this.propellantMass = propellantMass;
        this.currentOrbit = currentOrbit;
        this.targetOrbit = targetOrbit;
    }

    public String getName()
    {
        return name;
    }

    public double getDryMass()
    {
        return dryMass;
    }

    public double getPropellantMass()
    {
        return propellantMass;
    }

    public double getInitialMass()
    {
        return dryMass + propellantMass;
    }

    public double getCurrentMass()
    {
        return dryMass + propellantMass;
    }

    public Orbit getCurrentOrbit()
    {
        return currentOrbit;
    }

    public Orbit getTargetOrbit()
    {
        return targetOrbit;
    }

    public void setCurrentOrbit(Orbit currentOrbit)
    {
        if (currentOrbit == null)
        {
            throw new IllegalArgumentException(
                "Current orbit cannot be null."
            );
        }

        this.currentOrbit = currentOrbit;
    }

    public void consumePropellant(double amount)
    {
        if (amount < 0)
        {
            throw new IllegalArgumentException(
                "Propellant consumption cannot be negative."
            );
        }

        if (amount > propellantMass)
        {
            throw new IllegalArgumentException(
                "Not enough propellant available."
            );
        }

        propellantMass -= amount;
    }
}