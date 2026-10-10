public class Propulsion
{
    private final String engineName;
    private final double specificImpulse;
    private final double thrust;
    private final double efficiency;

    public Propulsion(String engineName, double specificImpulse, double thrust, double efficiency)
    {
        if (engineName == null || engineName.trim().isEmpty()) throw new IllegalArgumentException("Engine name cannot be empty.");
        if (!Double.isFinite(specificImpulse) || specificImpulse <= 0.0) throw new IllegalArgumentException("Specific impulse must be finite and positive.");
        if (!Double.isFinite(thrust) || thrust <= 0.0) throw new IllegalArgumentException("Thrust must be finite and positive.");
        if (!Double.isFinite(efficiency) || efficiency <= 0.0 || efficiency > 1.0) throw new IllegalArgumentException("Efficiency must be greater than zero and at most one.");
        this.engineName = engineName.trim();
        this.specificImpulse = specificImpulse;
        this.thrust = thrust;
        this.efficiency = efficiency;
        if (!Double.isFinite(getEffectiveThrust()) || getEffectiveThrust() <= 0.0) throw new IllegalArgumentException("Effective thrust must be finite and positive.");
    }

    public String getEngineName() { return engineName; }
    public double getSpecificImpulse() { return specificImpulse; }
    public double getThrust() { return thrust; }
    public double getEfficiency() { return efficiency; }
    public double getEffectiveThrust() { return thrust * efficiency; }

    public double calculateExhaustVelocity()
    {
        double velocity = specificImpulse * Constants.STANDARD_GRAVITY;
        if (!Double.isFinite(velocity) || velocity <= 0.0) throw new ArithmeticException("Effective exhaust velocity is invalid.");
        return velocity;
    }

    public double calculateMassFlowRate()
    {
        double flow = getEffectiveThrust() / calculateExhaustVelocity();
        if (!Double.isFinite(flow) || flow <= 0.0) throw new ArithmeticException("Mass flow rate is invalid.");
        return flow;
    }

    public double calculateBurnTime(double propellantMass)
    {
        if (!Double.isFinite(propellantMass) || propellantMass < 0.0) throw new IllegalArgumentException("Propellant mass must be finite and non-negative.");
        return propellantMass / calculateMassFlowRate();
    }
}
