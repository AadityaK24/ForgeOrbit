public class InputValidator
{
    public boolean isValidMissionName(String missionName)
    {
        return missionName != null
            && !missionName.trim().isEmpty();
    }

    public boolean isValidAltitude(double altitude)
    {
        return altitude > 0;
    }

    public boolean isValidMass(double mass)
    {
        return mass > 0;
    }

    public boolean isValidPropellantMass(double propellantMass)
    {
        return propellantMass >= 0;
    }

    public boolean isValidSpecificImpulse(double specificImpulse)
    {
        return specificImpulse > 0;
    }

    public boolean isValidThrust(double thrust)
    {
        return thrust > 0;
    }

    public boolean isValidEfficiency(double efficiency)
    {
        return efficiency > 0 && efficiency <= 1;
    }

    public boolean areValidOrbitAltitudes(
        double initialAltitude,
        double targetAltitude)
    {
        return initialAltitude > 0
            && targetAltitude > 0
            && initialAltitude != targetAltitude;
    }

    public boolean isValidSatelliteData(
        double dryMass,
        double propellantMass)
    {
        return dryMass > 0
            && propellantMass >= 0;
    }

    public boolean isValidPropulsionData(
        double specificImpulse,
        double thrust,
        double efficiency)
    {
        return specificImpulse > 0
            && thrust > 0
            && efficiency > 0
            && efficiency <= 1;
    }
}