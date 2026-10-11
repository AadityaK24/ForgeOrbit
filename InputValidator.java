public class InputValidator
{
    public InputValidator() { }

    public boolean isValidMissionName(String value)
    {
        return value != null && value.trim().matches("[A-Za-z0-9][A-Za-z0-9 _.-]{0,79}");
    }

    public boolean isValidAltitude(double value)
    {
        return Double.isFinite(value) && value > 0.0;
    }

    public boolean areValidOrbitAltitudes(double initialAltitude, double targetAltitude)
    {
        return isValidAltitude(initialAltitude) && isValidAltitude(targetAltitude)
            && Math.abs(initialAltitude - targetAltitude) > 1.0e-9;
    }

    public boolean isValidMass(double value)
    {
        return Double.isFinite(value) && value > 0.0;
    }

    public boolean isValidPropellantMass(double value)
    {
        return Double.isFinite(value) && value >= 0.0;
    }

    public boolean isValidSpecificImpulse(double value)
    {
        return Double.isFinite(value) && value > 0.0;
    }

    public boolean isValidThrust(double value)
    {
        return Double.isFinite(value) && value > 0.0;
    }

    public boolean isValidEfficiency(double value)
    {
        return Double.isFinite(value) && value > 0.0 && value <= 1.0;
    }

    public static double parseFiniteDouble(String text, String fieldName)
    {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException(fieldName + " is required.");
        final double value;
        try { value = Double.parseDouble(text.trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(fieldName + " must be a number."); }
        if (!Double.isFinite(value)) throw new IllegalArgumentException(fieldName + " must be finite.");
        return value;
    }

    public static double parsePositiveDouble(String text, String fieldName)
    {
        double value = parseFiniteDouble(text, fieldName);
        if (value <= 0.0) throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        return value;
    }

    public static double parseNonNegativeDouble(String text, String fieldName)
    {
        double value = parseFiniteDouble(text, fieldName);
        if (value < 0.0) throw new IllegalArgumentException(fieldName + " cannot be negative.");
        return value;
    }

    public static int parsePositiveInteger(String text, String fieldName)
    {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException(fieldName + " is required.");
        try
        {
            int value = Integer.parseInt(text.trim());
            if (value <= 0) throw new IllegalArgumentException(fieldName + " must be greater than zero.");
            return value;
        }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(fieldName + " must be a whole number greater than zero."); }
    }

    public static boolean isValidPositiveNumber(String text)
    {
        try { return parsePositiveDouble(text, "Value") > 0.0; }
        catch (IllegalArgumentException ex) { return false; }
    }

    public static void requireNonEmpty(String text, String fieldName)
    {
        if (text == null || text.trim().isEmpty()) throw new IllegalArgumentException(fieldName + " cannot be empty.");
    }
}
