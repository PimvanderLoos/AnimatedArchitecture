package nl.pim16aap2.animatedarchitecture.core.util;

/**
 * Collections of Maths-related utilities.
 */
@SuppressWarnings("unused")
public final class MathUtil
{
    public static final double HALF_PI = Math.PI / 2;
    public static final double EPS = 2 * Double.MIN_VALUE;

    private MathUtil()
    {
    }

    /**
     * Returns the largest integer value that is less than or equal to the argument.
     * <p>
     * See {@link Math#floor(double)}.
     *
     * @param value
     *     the value to round down to the nearest integer.
     * @return the largest integer value that is less than or equal to the argument.
     */
    public static int floor(double value)
    {
        return (int) Math.floor(value);
    }

    /**
     * Returns the closest integer to the argument. The result is rounded to an integer if the argument is not already
     * an integer.
     * <p>
     * See {@link Math#round(double)}.
     *
     * @param value
     *     the value to round to the nearest integer.
     * @return the closest integer to the argument.
     */
    public static int round(double value)
    {
        return (int) Math.round(value);
    }

    /**
     * Returns the smallest integer value that is greater than or equal to the argument.
     * <p>
     * See {@link Math#ceil(double)}.
     *
     * @param value
     *     The value to round up to the nearest integer.
     * @return The smallest integer value that is greater than or equal to the argument.
     */
    public static int ceil(double value)
    {
        return (int) Math.ceil(value);
    }

    /**
     * Clamps a value between minimum and maximum values.
     *
     * @param val
     *     The value to clamp.
     * @param minVal
     *     The lower bound value of the range to which the value should be clamped.
     * @param maxVal
     *     The upper bound value of the range to which the value should be clamped.
     * @return The input value if it is less than the maximum value and more than the minimum value, or the minimum or
     * maximum values if it exceeds either of those.
     */
    public static double clamp(double val, double minVal, double maxVal)
    {
        return Math.min(Math.max(val, minVal), maxVal);
    }

    /**
     * Clamps a value between minimum and maximum values.
     *
     * @param val
     *     The value to clamp.
     * @param minVal
     *     The lower bound value of the range to which the value should be clamped.
     * @param maxVal
     *     The upper bound value of the range to which the value should be clamped.
     * @return The input value if it is less than the maximum value and more than the minimum value, or the minimum or
     * maximum values if it exceeds either of those.
     */
    public static int clamp(int val, int minVal, int maxVal)
    {
        return Math.min(Math.max(val, minVal), maxVal);
    }
}
