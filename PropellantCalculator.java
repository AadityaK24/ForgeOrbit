
public class PropellantCalculator
{
    public double calculateRequiredPropellant(
        double deltaV,
        double dryMass,
        Propulsion propulsion)
    {
        validateDeltaV(deltaV);
        validatePositiveMass(dryMass);
        validatePropulsion(propulsion);

        double exhaustVelocity =
            propulsion.calculateExhaustVelocity();

        double exponent = deltaV / exhaustVelocity;

        double propellantMass =
            dryMass * Math.expm1(exponent);

        if (!Double.isFinite(propellantMass)
                || propellantMass < 0.0)
        {
            throw new ArithmeticException(
                "Required propellant exceeds the numeric range."
            );
        }

        return propellantMass;
    }

    public double calculateFinalMass(
        double initialMass,
        double deltaV,
        Propulsion propulsion)
    {
        validatePositiveMass(initialMass);
        validateDeltaV(deltaV);
        validatePropulsion(propulsion);

        double exponent = deltaV
            / propulsion.calculateExhaustVelocity();

        double finalMass =
            initialMass * Math.exp(-exponent);

        if (!Double.isFinite(finalMass) || finalMass <= 0.0)
        {
            throw new ArithmeticException(
                "Final mass is outside the representable range."
            );
        }

        return finalMass;
    }

    public double calculateAvailableDeltaV(
        double dryMass,
        double propellantMass,
        Propulsion propulsion)
    {
        validatePositiveMass(dryMass);
        validatePropellantMass(propellantMass);
        validatePropulsion(propulsion);

        if (propellantMass == 0.0)
        {
            return 0.0;
        }

        double ratio = propellantMass / dryMass;

        double logarithmicMassRatio;

        if (Double.isFinite(ratio))
        {
            logarithmicMassRatio = Math.log1p(ratio);
        }
        else
        {
            logarithmicMassRatio =
                Math.log(propellantMass) - Math.log(dryMass);
        }

        double deltaV =
            propulsion.calculateExhaustVelocity()
            * logarithmicMassRatio;

        if (!Double.isFinite(deltaV) || deltaV < 0.0)
        {
            throw new ArithmeticException(
                "Available delta-v is outside the numeric range."
            );
        }

        return deltaV;
    }

    public double calculatePropellantFraction(
        double dryMass,
        double propellantMass)
    {
        validatePositiveMass(dryMass);
        validatePropellantMass(propellantMass);

        if (propellantMass == 0.0)
        {
            return 0.0;
        }

        if (propellantMass <= dryMass)
        {
            double ratio = propellantMass / dryMass;

            return ratio / (1.0 + ratio);
        }

        double ratio = dryMass / propellantMass;

        return 1.0 / (1.0 + ratio);
    }

    private void validateDeltaV(double deltaV)
    {
        if (!Double.isFinite(deltaV) || deltaV < 0.0)
        {
            throw new IllegalArgumentException(
                "Delta-v must be finite and non-negative."
            );
        }
    }

    private void validatePositiveMass(double mass)
    {
        if (!Double.isFinite(mass) || mass <= 0.0)
        {
            throw new IllegalArgumentException(
                "Mass must be finite and positive."
            );
        }
    }

    private void validatePropellantMass(double mass)
    {
        if (!Double.isFinite(mass) || mass < 0.0)
        {
            throw new IllegalArgumentException(
                "Propellant mass must be finite and non-negative."
            );
        }
    }

    private void validatePropulsion(Propulsion propulsion)
    {
        if (propulsion == null)
        {
            throw new IllegalArgumentException(
                "Propulsion system cannot be null."
            );
        }
    }
}
