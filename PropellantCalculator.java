public class PropellantCalculator
{
    public double calculateRequiredPropellant(
        double deltaV,
        double dryMass,
        Propulsion propulsion)
    {
        if (deltaV < 0)
        {
            throw new IllegalArgumentException(
                "Delta-v cannot be negative."
            );
        }

        if (dryMass <= 0)
        {
            throw new IllegalArgumentException(
                "Dry mass must be greater than 0."
            );
        }

        if (propulsion == null)
        {
            throw new IllegalArgumentException(
                "Propulsion system cannot be null."
            );
        }

        double exhaustVelocity =
            propulsion.calculateExhaustVelocity();

        double massRatio =
            Math.exp(deltaV / exhaustVelocity);

        double initialMass = dryMass * massRatio;

        return initialMass - dryMass;
    }

    public double calculateFinalMass(
        double initialMass,
        double deltaV,
        Propulsion propulsion)
    {
        if (initialMass <= 0)
        {
            throw new IllegalArgumentException(
                "Initial mass must be greater than 0."
            );
        }

        if (deltaV < 0)
        {
            throw new IllegalArgumentException(
                "Delta-v cannot be negative."
            );
        }

        if (propulsion == null)
        {
            throw new IllegalArgumentException(
                "Propulsion system cannot be null."
            );
        }

        double exhaustVelocity =
            propulsion.calculateExhaustVelocity();

        return initialMass /
            Math.exp(deltaV / exhaustVelocity);
    }

    public double calculateAvailableDeltaV(
        double dryMass,
        double propellantMass,
        Propulsion propulsion)
    {
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

        if (propulsion == null)
        {
            throw new IllegalArgumentException(
                "Propulsion system cannot be null."
            );
        }

        double initialMass = dryMass + propellantMass;
        double exhaustVelocity =
            propulsion.calculateExhaustVelocity();

        return exhaustVelocity *
            Math.log(initialMass / dryMass);
    }

    public double calculatePropellantFraction(
        double dryMass,
        double propellantMass)
    {
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

        double initialMass = dryMass + propellantMass;

        return propellantMass / initialMass;
    }
}