public class HohmannTransfer
{
    private final Earth earth;
    private final OrbitalMechanics mechanics;

    public HohmannTransfer() { this(new Earth()); }
    public HohmannTransfer(Earth earth) { this(earth, new OrbitalMechanics(earth)); }

    public HohmannTransfer(Earth earth, OrbitalMechanics mechanics)
    {
        if (earth == null || mechanics == null) throw new IllegalArgumentException("Earth and orbital mechanics cannot be null.");
        this.earth = earth;
        this.mechanics = mechanics;
    }

    public TransferResult calculateTransfer(Orbit initialOrbit, Orbit targetOrbit)
    {
        validateOrbit(initialOrbit, "Initial orbit");
        validateOrbit(targetOrbit, "Target orbit");
        double r1 = initialOrbit.getRadius();
        double r2 = targetOrbit.getRadius();
        double a = (r1 + r2) / 2.0;
        if (Math.abs(r2 - r1) <= Math.max(r1, r2) * 1e-12)
        {
            return new TransferResult(a, 0.0, 0.0, 0.0, 0.0);
        }
        double mu = earth.getGravitationalParameter();
        double v1 = Math.sqrt(mu / r1);
        double v2 = Math.sqrt(mu / r2);
        double transferVelocity1 = Math.sqrt(mu * (2.0 / r1 - 1.0 / a));
        double transferVelocity2 = Math.sqrt(mu * (2.0 / r2 - 1.0 / a));
        double burn1 = Math.abs(transferVelocity1 - v1);
        double burn2 = Math.abs(v2 - transferVelocity2);
        double total = burn1 + burn2;
        double time = Math.PI * Math.sqrt((a * a * a) / mu);
        if (!Double.isFinite(total) || !Double.isFinite(time)) throw new ArithmeticException("Hohmann transfer result is invalid.");
        return new TransferResult(a, burn1, burn2, total, time);
    }

    public TransferResult calculateHohmannTransfer(Orbit initialOrbit, Orbit targetOrbit)
    {
        return calculateTransfer(initialOrbit, targetOrbit);
    }

    public OrbitalMechanics getOrbitalMechanics() { return mechanics; }
    public Earth getEarth() { return earth; }

    private void validateOrbit(Orbit orbit, String label)
    {
        if (orbit == null) throw new IllegalArgumentException(label + " cannot be null.");
        if (!Double.isFinite(orbit.getRadius()) || orbit.getRadius() <= 0.0) throw new IllegalArgumentException(label + " radius is invalid.");
    }
}
