public class Propulsion
{
    private final String engineName;
    private final double specificImpulse;
    private final double thrust;
    private final double efficiency;

    public Propulsion(
        String engineName,
        double specificImpulse,
        double thrust,
        double efficiency)
    {
        if (engineName == null || engineName.trim().isEmpty())
        {
            throw new IllegalArgumentException(
                "Engine name cannot be empty."
            );
        }

        if (specificImpulse <= 0)
        {
            throw new IllegalArgumentException(
                "Specific impulse must be greater than 0."
            );
        }

        if (thrust <= 0)
        {
            throw new IllegalArgumentException(
                "Thrust must be greater than 0."
            );
        }

        if (efficiency <= 0 || efficiency > 1)
        {
            throw new IllegalArgumentException(
                "Efficiency must be between 0 and 1."
            );
        }

        this.engineName = engineName;
        this.specificImpulse = specificImpulse;
        this.thrust = thrust;
        this.efficiency = efficiency;
    }

    public String getEngineName()
    {
        return engineName;
    }

    public double getSpecificImpulse()
    {
        return specificImpulse;
    }

    public double getThrust()
    {
        return thrust;
    }

    public double getEfficiency()
    {
        return efficiency;
    }

    public double calculateExhaustVelocity()
    {
        return specificImpulse * Constants.STANDARD_GRAVITY;
    }

    public double calculateMassFlowRate()
    {
        return thrust / calculateExhaustVelocity();
    }

    public double calculateBurnTime(
        double propellantMass)
    {
        if (propellantMass < 0)
        {
            throw new IllegalArgumentException(
                "Propellant mass cannot be negative."
            );
        }

        double massFlowRate = calculateMassFlowRate();

        return propellantMass / massFlowRate;
    }
}