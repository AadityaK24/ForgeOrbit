
public class InputValidator
{
    public boolean isValidMissionName(String missionName)
    {
        return missionName != null
            && !missionName.trim().isEmpty();
    }

    public boolean isValidAltitude(double altitude)
    {
        return Double.isFinite(altitude) && altitude > 0.0;
    }

    public boolean isValidMass(double mass)
    {
        return Double.isFinite(mass) && mass > 0.0;
    }

    public boolean isValidPropellantMass(double propellantMass)
    {
        return Double.isFinite(propellantMass)
            && propellantMass >= 0.0;
    }

    public boolean isValidSpecificImpulse(double specificImpulse)
    {
        return Double.isFinite(specificImpulse)
            && specificImpulse > 0.0;
    }

    public boolean isValidThrust(double thrust)
    {
        return Double.isFinite(thrust) && thrust > 0.0;
    }

    public boolean isValidEfficiency(double efficiency)
    {
        return Double.isFinite(efficiency)
            && efficiency > 0.0
            && efficiency <= 1.0;
    }

    public boolean areValidOrbitAltitudes(
        double initialAltitude,
        double targetAltitude)
    {
        return isValidAltitude(initialAltitude)
            && isValidAltitude(targetAltitude)
            && initialAltitude != targetAltitude;
    }

    public boolean isValidSatelliteData(
        double dryMass,
        double propellantMass)
    {
        return isValidMass(dryMass)
            && isValidPropellantMass(propellantMass)
            && Double.isFinite(dryMass + propellantMass);
    }

    public boolean isValidPropulsionData(
        double specificImpulse,
        double thrust,
        double efficiency)
    {
        return isValidSpecificImpulse(specificImpulse)
            && isValidThrust(thrust)
            && isValidEfficiency(efficiency);
    }
}
