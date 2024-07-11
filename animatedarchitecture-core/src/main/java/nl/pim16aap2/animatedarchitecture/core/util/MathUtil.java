package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.experimental.UtilityClass;
import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Utility class for mathematical operations.
 */
@Flogger
@UtilityClass
public final class MathUtil
{
    /**
     * Half of the mathematical constant PI.
     */
    public static final double HALF_PI = Math.PI / 2;

    /**
     * Epsilon value for floating point comparisons.
     */
    public static final double EPS = 2 * Double.MIN_VALUE;

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

    /**
     * Clamp an angle to [-2PI ; 2PI].
     *
     * @param angle
     *     The current angle in radians.
     * @return The angle (in radians) clamped to [-2PI ; 2PI].
     */
    public static double clampAngleRad(double angle)
    {
        return angle % Math.TAU;
    }

    /**
     * Clamp an angle to [-360 ; 360].
     *
     * @param angle
     *     The current angle in degrees.
     * @return The angle (in degrees) clamped to [-360 ; 360].
     */
    public static double clampAngleDeg(double angle)
    {
        return angle % 360;
    }

    /**
     * Parses a string to an integer.
     *
     * @param str
     *     The string to parse.
     * @return The parsed integer, or an empty {@link OptionalInt} if the string was null or could not be parsed.
     */
    public static OptionalInt parseInt(@Nullable String str)
    {
        if (str == null)
            return OptionalInt.empty();

        try
        {
            return OptionalInt.of(Integer.parseInt(str));
        }
        catch (NumberFormatException e)
        {
            return OptionalInt.empty();
        }
    }

    /**
     * Parses an optional string to an integer.
     *
     * @param str
     *     The string to parse.
     * @return The parsed integer, or an empty {@link OptionalInt} if the optional was empty or the string could not be
     * parsed.
     */
    public static OptionalInt parseInt(Optional<String> str)
    {
        return str.map(MathUtil::parseInt).orElse(OptionalInt.empty());
    }

    /**
     * Parses a string to a double.
     *
     * @param str
     *     The string to parse.
     * @return The parsed double, or an empty {@link OptionalDouble} if the string was null or could not be parsed.
     */
    public static OptionalDouble parseDouble(@Nullable String str)
    {
        if (str == null)
            return OptionalDouble.empty();

        try
        {
            return OptionalDouble.of(Double.parseDouble(str));
        }
        catch (NumberFormatException e)
        {
            return OptionalDouble.empty();
        }
    }

    /**
     * Parses an optional string to a double.
     *
     * @param str
     *     The string to parse.
     * @return The parsed double, or an empty {@link OptionalDouble} if the optional was empty or the string could not
     * be parsed.
     */
    public static OptionalDouble parseDouble(Optional<String> str)
    {
        return str.map(MathUtil::parseDouble).orElse(OptionalDouble.empty());
    }

    /**
     * Parses a string to a long.
     *
     * @param str
     *     The string to parse.
     * @return The parsed long, or an empty {@link OptionalLong} if the string was null or could not be parsed.
     */
    public static OptionalLong parseLong(@Nullable String str)
    {
        if (str == null)
            return OptionalLong.empty();

        try
        {
            return OptionalLong.of(Long.parseLong(str));
        }
        catch (NumberFormatException e)
        {
            return OptionalLong.empty();
        }
    }

    /**
     * Parses an optional string to a long.
     *
     * @param str
     *     The string to parse.
     * @return The parsed long, or an empty {@link OptionalLong} if the optional was empty or the string could not be
     * parsed.
     */
    public static OptionalLong parseLong(Optional<String> str)
    {
        return str.map(MathUtil::parseLong).orElse(OptionalLong.empty());
    }

    /**
     * Checks if a string is numerical integer with radix 10.
     * <p>
     * Any string for which this method returns false will always throw a {@link NumberFormatException} when parsed by
     * {@link Integer#parseInt(String)}.
     * <p>
     * Conversely, any string for which this method returns true may still throw a {@link NumberFormatException} when
     * parsed by {@link Integer#parseInt(String)} if the number is too large.
     *
     * @param str
     *     The string to check.
     * @return True if the string is numerical.
     */
    public static boolean isNumerical(@Nullable String str)
    {
        if (str == null || str.isEmpty())
            return false;

        int idx = 0;

        // If the first character is a minus sign, swap the sign of the limit and move to the next character.
        // If the string is only a minus sign, it is not numerical.
        if (str.charAt(idx) == '-')
        {
            if (str.length() == 1)
                return false;
            ++idx;
        }
        for (; idx < str.length(); ++idx)
        {
            final int digit = Character.digit(str.charAt(idx), 10);
            if (digit < 0)
                return false;
        }
        return true;
    }

    /**
     * Check if a given value is between two other values. Matches inclusively.
     *
     * @param test
     *     Value to be compared.
     * @param low
     *     Minimum value.
     * @param high
     *     Maximum value.
     * @return True if the value is in the provided range or if it equals the low and/or the high value.
     */
    public static boolean between(int test, int low, int high)
    {
        return test <= high && test >= low;
    }

    /**
     * Check if a given value is between two other values. Matches inclusively.
     *
     * @param test
     *     Value to be compared.
     * @param low
     *     Minimum value.
     * @param high
     *     Maximum value.
     * @return True if the value is in the provided range or if it equals the low and/or the high value.
     */
    public static boolean between(double test, double low, double high)
    {
        return test <= high && test >= low;
    }
}
