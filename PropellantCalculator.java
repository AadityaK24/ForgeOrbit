public class PropellantCalculator
{
    public double calculateRequiredPropellant(double deltaV, double dryMass, Propulsion propulsion)
    {
        validateDeltaV(deltaV);
        validatePositiveMass(dryMass);
        validatePropulsion(propulsion);
        double exponent = deltaV / propulsion.calculateExhaustVelocity();
        double propellantMass = dryMass * Math.expm1(exponent);
        if (!Double.isFinite(propellantMass) || propellantMass < 0.0) throw new ArithmeticException("Required propellant exceeds the numeric range.");
        return propellantMass;
    }

    public double calculateFinalMass(double initialMass, double deltaV, Propulsion propulsion)
    {
        validatePositiveMass(initialMass);
        validateDeltaV(deltaV);
        validatePropulsion(propulsion);
        double finalMass = initialMass * Math.exp(-deltaV / propulsion.calculateExhaustVelocity());
        if (!Double.isFinite(finalMass) || finalMass <= 0.0) throw new ArithmeticException("Final mass is outside the representable range.");
        return finalMass;
    }

    public double calculateAvailableDeltaV(double dryMass, double propellantMass, Propulsion propulsion)
    {
        validatePositiveMass(dryMass);
        validatePropellantMass(propellantMass);
        validatePropulsion(propulsion);
        if (propellantMass == 0.0) return 0.0;
        double ratio = propellantMass / dryMass;
        double logRatio = Double.isFinite(ratio) ? Math.log1p(ratio) : Math.log(propellantMass) - Math.log(dryMass);
        double deltaV = propulsion.calculateExhaustVelocity() * logRatio;
        if (!Double.isFinite(deltaV) || deltaV < 0.0) throw new ArithmeticException("Available delta-v is outside the numeric range.");
        return deltaV;
    }

    public double calculatePropellantFraction(double dryMass, double propellantMass)
    {
        validatePositiveMass(dryMass);
        validatePropellantMass(propellantMass);
        if (propellantMass == 0.0) return 0.0;
        if (propellantMass <= dryMass)
        {
            double ratio = propellantMass / dryMass;
            return ratio / (1.0 + ratio);
        }
        double ratio = dryMass / propellantMass;
        return 1.0 / (1.0 + ratio);
    }

    private void validateDeltaV(double value)
    {
        if (!Double.isFinite(value) || value < 0.0) throw new IllegalArgumentException("Delta-v must be finite and non-negative.");
    }
    private void validatePositiveMass(double value)
    {
        if (!Double.isFinite(value) || value <= 0.0) throw new IllegalArgumentException("Mass must be finite and positive.");
    }
    private void validatePropellantMass(double value)
    {
        if (!Double.isFinite(value) || value < 0.0) throw new IllegalArgumentException("Propellant mass must be finite and non-negative.");
    }
    private void validatePropulsion(Propulsion value)
    {
        if (value == null) throw new IllegalArgumentException("Propulsion system cannot be null.");
    }
}
