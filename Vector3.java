public final class Vector3
{
    private final double x;
    private final double y;
    private final double z;

    public Vector3(double x, double y, double z)
    {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
        {
            throw new IllegalArgumentException("Vector components must be finite.");
        }
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public Vector3 add(Vector3 other)
    {
        requireVector(other);
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 subtract(Vector3 other)
    {
        requireVector(other);
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public Vector3 scale(double factor)
    {
        if (!Double.isFinite(factor))
        {
            throw new IllegalArgumentException("Scale factor must be finite.");
        }
        return new Vector3(x * factor, y * factor, z * factor);
    }

    public double dot(Vector3 other)
    {
        requireVector(other);
        return x * other.x + y * other.y + z * other.z;
    }

    public double magnitude()
    {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double distanceTo(Vector3 other)
    {
        return subtract(other).magnitude();
    }

    public Vector3 normalized()
    {
        double magnitude = magnitude();
        if (magnitude == 0.0)
        {
            throw new IllegalStateException("A zero vector cannot be normalized.");
        }
        return scale(1.0 / magnitude);
    }

    private static void requireVector(Vector3 vector)
    {
        if (vector == null)
        {
            throw new IllegalArgumentException("Vector cannot be null.");
        }
    }
}
