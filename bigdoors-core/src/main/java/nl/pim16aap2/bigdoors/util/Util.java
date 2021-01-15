package nl.pim16aap2.bigdoors.util;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents various small and platform agnostic utility functions.
 *
 * @author Pim
 */
@UtilityClass
public final class Util
{
    /**
     * Characters to use in (secure) random strings.
     */
    @NotNull
    private static final String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Used to generate secure random strings. It's more secure than {@link Util#rnd}, but slower.
     */
    @NotNull
    private static final SecureRandom srnd = new SecureRandom();

    /**
     * Used to generate simple random strings. It's faster than {@link Util#srnd}, but not secure.
     */
    @NotNull
    private static final Random rnd = new Random();

    @NotNull
    private static final Map<PBlockFace, RotateDirection> toRotateDirection = new EnumMap<>(PBlockFace.class);
    @NotNull
    private static final Map<RotateDirection, PBlockFace> toPBlockFace = new EnumMap<>(RotateDirection.class);

    static
    {
        for (final @NotNull PBlockFace pbf : PBlockFace.values())
        {
            RotateDirection mappedRotDir;
            try
            {
                mappedRotDir = RotateDirection.valueOf(pbf.toString());
            }
            catch (IllegalArgumentException e)
            {
                mappedRotDir = RotateDirection.NONE;
            }
            toRotateDirection.put(pbf, mappedRotDir);
            toPBlockFace.put(mappedRotDir, pbf);
        }
    }

    public static @NotNull OptionalInt parseInt(final @Nullable String str)
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

    public static @NotNull OptionalInt parseInt(final @NonNull Optional<String> str)
    {
        return str.map(Util::parseInt).orElse(OptionalInt.empty());
    }

    public static @NotNull OptionalDouble parseDouble(final @Nullable String str)
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

    public static @NotNull OptionalDouble parseDouble(final @NonNull Optional<String> str)
    {
        return str.map(Util::parseDouble).orElse(OptionalDouble.empty());
    }

    public static @NotNull OptionalLong parseLong(final @Nullable String str)
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

    public static @NotNull OptionalLong parseLong(final @NonNull Optional<String> str)
    {
        return str.map(Util::parseLong).orElse(OptionalLong.empty());
    }

    /**
     * Gets a {@link NonNull} value from a {@link Nullable} one, with a provided fallback in case the value is null.
     *
     * @param value    The value that may or may not be null.
     * @param fallback A {@link Supplier} to supply a fallback to return in case the value is null.
     * @param <T>      The type of the value.
     * @return The value if it is not null, otherwise the fallback.
     */
    public @NonNull <T> T valOrDefault(final @Nullable T value, final @NonNull Supplier<T> fallback)
    {
        return value == null ? fallback.get() : value;
    }

    /**
     * Searches through an {@link Iterable} object using a provided search function.
     *
     * @param iterable   The {@link Iterable} object to search through.
     * @param searchPred The search predicate to use.
     * @param <T>        The type of objects stored in the {@link Iterable}.
     * @return The value in the {@link Iterable} object for which the search function returns true, otherwise {@link
     * Optional#empty()}.
     */
    public @NonNull <T> Optional<T> searchIterable(final @NonNull Iterable<T> iterable,
                                                   final @NonNull Predicate<T> searchPred)
    {
        for (final @NonNull T val : iterable)
            if (searchPred.test(val))
                return Optional.of(val);
        return Optional.empty();
    }

    /**
     * Obtains the numbers of question marks in a String.
     *
     * @param statement The String.
     * @return The number of question marks in the String.
     */
    public static int countPatternOccurrences(final @NotNull Pattern pattern, final @NotNull String statement)
    {
        int found = 0;
        final Matcher matcher = pattern.matcher(statement);
        while (matcher.find())
            ++found;
        return found;
    }

    /**
     * Gets the {@link RotateDirection} equivalent of a {@link PBlockFace} if it exists.
     *
     * @param pBlockFace The {@link PBlockFace}.
     * @return The {@link RotateDirection} equivalent of a {@link PBlockFace} if it exists and otherwise {@link
     * RotateDirection#NONE}.
     */
    public static @NotNull RotateDirection getRotateDirection(final @NotNull PBlockFace pBlockFace)
    {
        return toRotateDirection.get(pBlockFace);
    }

    /**
     * Gets the {@link PBlockFace} equivalent of a {@link RotateDirection} if it exists.
     *
     * @param rotateDirection The {@link RotateDirection}.
     * @return The {@link PBlockFace} equivalent of a {@link RotateDirection} if it exists and otherwise {@link
     * PBlockFace#NONE}.
     */
    public static @NotNull PBlockFace getPBlockFace(final @NotNull RotateDirection rotateDirection)
    {
        return toPBlockFace.get(rotateDirection);
    }

    /**
     * Capitalizes the first letter. The rest of the String is left intact.
     *
     * @param string The string for which to capitalize the first letter.
     * @return The same string that it received as input, but with a capizalized first letter.
     */
    public static @NotNull String capitalizeFirstLetter(final @NotNull String string)
    {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    /**
     * Clamp an angle to [-2PI ; 2PI].
     *
     * @param angle The current angle in radians.
     * @return The angle (in radians) clamped to [-2PI ; 2PI].
     */
    public static double clampAngleRad(final double angle)
    {
        return angle % (2 * Math.PI);
//        double twoPi = 2 * Math.PI;
//        return (angle + twoPi) % twoPi;
    }

    /**
     * Clamp an angle to [-360 ; 360].
     *
     * @param angle The current angle in degrees.
     * @return The angle (in degrees) clamped to [-360 ; 360].
     */
    public static double clampAngleDeg(final double angle)
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
    public static @NotNull <T> T[] concatArrays(final @NotNull T[] first, final @NotNull T[] second)
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
    public static @NotNull <T> T[] doubleArraySize(final @NotNull T[] arr)
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
    public static @NotNull <T> T[] truncateArray(final @NotNull T[] arr, final int newLength)
    {
        return Arrays.copyOf(arr, newLength);
    }

    /**
     * Check if a given string is a valid door name. Numerical names aren't allowed to make sure they don't get confused
     * for doorUIDs.
     *
     * @param name The name to test for validity,
     * @return True if the name is allowed.
     */
    public static boolean isValidDoorName(final @NotNull String name)
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
    public static @NotNull String randomInsecureString(final int length)
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
    public static @NotNull String secureRandomString(final int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for (int idx = 0; idx != length; ++idx)
            sb.append(chars.charAt(srnd.nextInt(chars.length())));
        return sb.toString();
    }

    public static boolean hasPermissionForAction(final @NotNull UUID uuid, final @NotNull AbstractDoorBase door,
                                                 final @NotNull DoorAttribute attribute)
    {
        return door.getDoorOwner(uuid)
                   .map(doorOwner -> doorOwner.getPermission() <= DoorAttribute.getPermissionLevel(attribute))
                   .orElse(false);
    }

    public static boolean hasPermissionForAction(final @NotNull IPPlayer player, final @NotNull AbstractDoorBase door,
                                                 final @NotNull DoorAttribute attribute)
    {
        return hasPermissionForAction(player.getUUID(), door, attribute);
    }

    /**
     * Obtains a random integer value.
     *
     * @param min The lower bound (inclusive).
     * @param max The lower bound (inclusive).
     * @return A random integer value.
     */
    public static int getRandomNumber(final int min, final int max)
    {

        if (min >= max)
        {
            throw new IllegalArgumentException("max must be greater than min");
        }

        return rnd.nextInt((max - min) + 1) + min;
    }

    /**
     * Gets the chunk coordinates of a position.
     *
     * @param position The position.
     * @return The chunk coordinates.
     */
    public static @NotNull Vector2Di getChunkCoords(final @NotNull Vector3DiConst position)
    {
        return new Vector2Di(position.getX() << 4, position.getZ() << 4);
    }

    /**
     * Gets the 'simple' hash of the chunk given its coordinates. 'simple' here refers to the fact that the world of
     * this chunk will not be taken into account.
     *
     * @param chunkX The x-coordinate of the chunk.
     * @param chunkZ The z-coordinate of the chunk.
     * @return The simple hash of the chunk.
     */
    public static long simpleChunkHashFromChunkCoordinates(final int chunkX, final int chunkZ)
    {
        long hash = 3;
        hash = 19 * hash + (int) (Double.doubleToLongBits(chunkX) ^ (Double.doubleToLongBits(chunkX) >>> 32));
        hash = 19 * hash + (int) (Double.doubleToLongBits(chunkZ) ^ (Double.doubleToLongBits(chunkZ) >>> 32));
        return hash;
    }

    /**
     * Gets the 'simple' hash of the chunk that encompasses the given coordinates. 'simple' here refers to the fact that
     * the world of this chunk will not be taken into account.
     *
     * @param posX The x-coordinate of the location.
     * @param posZ The z-coordinate of the location.
     * @return The simple hash of the chunk.
     */
    public static long simpleChunkHashFromLocation(final int posX, final int posZ)
    {
        return simpleChunkHashFromChunkCoordinates(posX >> 4, posZ >> 4);
    }

    /**
     * Gets the 'simple' hash of a location. 'simple' here refers to the fact that the world of this location will not
     * be taken into account.
     *
     * @param x The x-coordinate of the location.
     * @param y The z-coordinate of the location.
     * @param z The z-coordinate of the location.
     * @return The simple hash of the location.
     */
    public static long simpleLocationhash(final int x, final int y, final int z)
    {
        int hash = 3;
        hash = 19 * hash + (int) (Double.doubleToLongBits(x) ^ Double.doubleToLongBits(x) >>> 32);
        hash = 19 * hash + (int) (Double.doubleToLongBits(y) ^ Double.doubleToLongBits(y) >>> 32);
        hash = 19 * hash + (int) (Double.doubleToLongBits(z) ^ Double.doubleToLongBits(z) >>> 32);
        return hash;
    }

    /**
     * Converts worldspace coordinates to chunkspace coordinates.
     *
     * @param position The position in world space coordinates.
     * @return The coordinates in chunkspace coordinates.
     */
    public static @NotNull Vector3Di getChunkSpacePosition(final @NotNull Vector3DiConst position)
    {
        return getChunkSpacePosition(position.getX(), position.getY(), position.getZ());
    }

    /**
     * Converts world space coordinates to chunk space coordinates.
     *
     * @param x The x coordinate in world space.
     * @param y The y coordinate in world space.
     * @param z The z coordinate in world space.
     * @return The coordinates in chunkspace coordinates.
     */
    public static @NotNull Vector3Di getChunkSpacePosition(final int x, final int y, final int z)
    {
        return new Vector3Di(x % 16, y, z % 16);
    }

    /**
     * Gets the 'simple' hash of a location in chunk-space. 'simple' here refers to the fact that the world of this
     * location will not be taken into account.
     *
     * @param x The x-coordinate of the location.
     * @param y The z-coordinate of the location.
     * @param z The z-coordinate of the location.
     * @return The simple hash of the location in chunk-space.
     */
    public static int simpleChunkSpaceLocationhash(final int x, final int y, final int z)
    {
        int chunkSpaceX = x % 16;
        int chunkSpaceZ = z % 16;
        return (y << 8) + (chunkSpaceX << 4) + chunkSpaceZ;
    }

    /**
     * Convert an array of strings to a single string.
     *
     * @param strings Input array of string
     * @return Resulting concatenated string.
     */
    public static @NotNull String stringFromArray(final @NotNull String[] strings)
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
    public static boolean between(final int test, final int low, final int high)
    {
        return test <= high && test >= low;
    }

    @Deprecated
    public static int tickRateFromSpeed(final double speed)
    {
        int tickRate;
        if (speed > 9)
            tickRate = 1;
        else if (speed > 7)
            tickRate = 2;
        else if (speed > 6)
            tickRate = 3;
        else
            tickRate = 4;
        return tickRate;
    }

    // Return {time, tickRate, distanceMultiplier} for a given door size.
    @Deprecated
    public static @NotNull double[] calculateTimeAndTickRate(final int doorSize, double time,
                                                             final double speedMultiplier,
                                                             final double baseSpeed)
    {
        final @NotNull double[] ret = new double[3];
        final double distance = Math.PI * doorSize / 2;
        if (time == 0.0)
            time = baseSpeed + doorSize / 3.5;
        double speed = distance / time;
        if (speedMultiplier != 1.0 && speedMultiplier != 0.0)
        {
            speed *= speedMultiplier;
            time = distance / speed;
        }

        // Too fast or too slow!
        final double maxSpeed = 11;
        if (speed > maxSpeed || speed <= 0)
            time = distance / maxSpeed;

        final double distanceMultiplier = speed > 4 ? 1.01 : speed > 3.918 ? 1.08 : speed > 3.916 ? 1.10 :
                                                                                    speed > 2.812 ? 1.12 :
                                                                                    speed > 2.537 ? 1.19 :
                                                                                    speed > 2.2 ? 1.22 :
                                                                                    speed > 2.0 ? 1.23 :
                                                                                    speed > 1.770 ?
                                                                                    1.25 :
                                                                                    speed > 1.570 ?
                                                                                    1.28 : 1.30;
        ret[0] = time;
        ret[1] = tickRateFromSpeed(speed);
        ret[2] = distanceMultiplier;
        return ret;
    }
}
