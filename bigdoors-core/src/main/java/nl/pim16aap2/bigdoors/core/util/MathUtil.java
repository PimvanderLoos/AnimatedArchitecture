package nl.pim16aap2.bigdoors.core.util;


@SuppressWarnings("unused")
public final class MathUtil
{
    public static final double HALF_PI = Math.PI / 2;
    public static final double EPS = 2 * Double.MIN_VALUE;

    private MathUtil()
    {
    }

    public static int floor(double value)
    {
        return (int) Math.floor(value);
    }

    public static int round(double value)
    {
        return (int) Math.round(value);
    }

    public static int ceil(double value)
    {
        return (int) Math.ceil(value);
    }
}
