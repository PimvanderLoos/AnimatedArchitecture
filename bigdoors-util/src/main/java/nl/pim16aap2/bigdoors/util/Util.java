package nl.pim16aap2.bigdoors.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

/**
 * Represents various small and platform agnostic utility functions.
 *
 * @author Pim
 */
public final class Util
{
    static final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static SecureRandom srnd = new SecureRandom();
    static Random rnd = new Random();

    /**
     * Clamp an angle to [-2PI ; 2PI].
     *
     * @param angle The current angle in radians.
     * @return The angle (in radians) clamped to [-2PI ; 2PI].
     */
    public static double clampAngleRad(double angle)
    {
        return angle % (2 * Math.PI);
    }

    /**
     * Clamp an angle to [-360 ; 360].
     *
     * @param angle The current angle in degrees.
     * @return The angle (in degrees) clamped to [-360 ; 360].
     */
    public static double clampAngleDeg(double angle)
    {
        return angle % (2 * Math.PI);
    }

    /**
     * Concatenate two arrays.
     *
     * @param <T>
     * @param first  First array.
     * @param second Second array.
     * @return A single concatenated array.
     */
    public static <T> T[] concatArrays(T[] first, T[] second)
    {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    /**
     * Double the size of a provided array
     *
     * @param <T>
     * @param arr Array to be doubled in size
     * @return A copy of the array but with doubled size.
     */
    public static <T> T[] doubleArraySize(T[] arr)
    {
        return Arrays.copyOf(arr, arr.length * 2);
    }

    /**
     * Truncate an array after a provided new length.
     *
     * @param <T>
     * @param arr       The array to truncate
     * @param newLength The new length of the array.
     * @return A truncated array
     */
    public static <T> T[] truncateArray(T[] arr, int newLength)
    {
        return Arrays.copyOf(arr, newLength);
    }

    /**
     * Check if a given string is a valid door name. Numerical names aren't allowed, to make sure they don't get
     * confused for doorUIDs.
     *
     * @param name The name to test for validity,
     * @return True if the name is allowed.
     */
    public static boolean isValidDoorName(String name)
    {
        try
        {
            Long.parseLong(name);
            return false;
        }
        catch (NumberFormatException e)
        {
            return true;
        }
    }

    /**
     * Generate an insecure random alphanumeric string of a given length.
     *
     * @param length Length of the resulting string
     * @return An insecure random alphanumeric string.
     */
    public static String randomInsecureString(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for (int idx = 0; idx != length; ++idx)
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    /**
     * Generate a secure random alphanumeric string of a given length.
     *
     * @param length Length of the resulting string
     * @return A secure random alphanumeric string.
     */
    public static String secureRandomString(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for (int idx = 0; idx != length; ++idx)
            sb.append(chars.charAt(srnd.nextInt(chars.length())));
        return sb.toString();
    }

    /**
     * Try to convert a string to a double. Use the default value in case of failure.
     *
     * @param input      The string to be converted to a double.
     * @param defaultVal The value that is to be used as backup.
     * @return Double converted from the string if possible, and defaultVal otherwise.
     */
    public static double doubleFromString(String input, double defaultVal)
    {
        try
        {
            return input == null ? defaultVal : Double.parseDouble(input);
        }
        catch (NumberFormatException e)
        {
            return defaultVal;
        }
    }

    /**
     * Try to convert a string to a long. Use the default value in case of failure.
     *
     * @param input      The string to be converted to a long.
     * @param defaultVal The value that is to be used as backup.
     * @return Long converted from the string if possible, and defaultVal otherwise.
     */
    public static long longFromString(String input, long defaultVal)
    {
        try
        {
            return input == null ? defaultVal : Long.parseLong(input);
        }
        catch (NumberFormatException e)
        {
            return defaultVal;
        }
    }

    /**
     * Convert an array of strings to a single string.
     *
     * @param strings Input array of string
     * @return Resulting concatenated string.
     */
    public static String stringFromArray(String[] strings)
    {
        StringBuilder builder = new StringBuilder();
        for (String str : strings)
            builder.append(str);
        return builder.toString();
    }

    /**
     * Check if a given value is between two other values. Matches inclusively.
     *
     * @param test Value to be compared.
     * @param low  Minimum value.
     * @param high Maximum value.
     * @return True if the value is in the provided range or if it equals the low and/or the high value.
     */
    public static boolean between(int test, int low, int high)
    {
        return test <= high && test >= low;
    }
}
