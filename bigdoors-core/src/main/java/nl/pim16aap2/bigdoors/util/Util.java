package nl.pim16aap2.bigdoors.util;

import lombok.experimental.UtilityClass;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.logging.PLogger;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Represents various small and platform-agnostic utility functions.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
@UtilityClass
public final class Util
{
    /**
     * Characters to use in (secure) random strings.
     */
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    /**
     * Used to generate secure random strings. It's more secure than {@link Util#RANDOM}, but slower.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Used to generate simple random strings. It's faster than {@link Util#SECURE_RANDOM}, but not secure.
     */
    private static final Random RANDOM = new Random();

    private static final Map<PBlockFace, RotateDirection> TO_ROTATE_DIRECTION =
        new EnumMap<>(PBlockFace.class);
    private static final Map<RotateDirection, PBlockFace> TO_PBLOCK_FACE =
        new EnumMap<>(RotateDirection.class);

    /**
     * Looks for top-level .properties files.
     */
    private static final Pattern LOCALE_FILE_PATTERN = Pattern.compile("^[\\w-]+\\.properties");

    static
    {
        for (val pbf : PBlockFace.values())
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
            TO_ROTATE_DIRECTION.put(pbf, mappedRotDir);
            TO_PBLOCK_FACE.put(mappedRotDir, pbf);
        }
    }

    /**
     * Gets the File of the jar that contained a specific class.
     *
     * @param clz
     *     The class for which to find the jar file.
     * @return The location of the jar file.
     */
    public Path getJarFile(Class<?> clz)
    {
        try
        {
            return Path.of(clz.getProtectionDomain().getCodeSource().getLocation().toURI());
        }
        catch (IllegalArgumentException | URISyntaxException e)
        {
            throw new RuntimeException("Failed to find jar file for class: " + clz, e);
        }
    }

    public List<String> getLocaleFilesInJar(Path jarFile)
        throws IOException
    {
        final List<String> ret = new ArrayList<>();

        try (val zipInputStream = new ZipInputStream(Files.newInputStream(jarFile)))
        {
            @Nullable ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null)
            {
                val name = entry.getName();
                if (LOCALE_FILE_PATTERN.matcher(name).matches())
                    ret.add(name);
            }
        }
        return ret;
    }

    /**
     * Ensures an object is not null.
     * <p>
     * If the object is null after all, a {@link NullPointerException} will be thrown.
     * <p>
     * This is basically the same as {@link Objects#requireNonNull(Object, String)} with the only difference being that
     * this will create a full "must not be null" message for the provided variable name.
     *
     * @param obj
     *     The object to check.
     * @param name
     *     The name of the input object. This is used in the NPE message with the format "{name} must not be null!".
     * @param <T>
     *     The type of the input object.
     * @return The input object, if it is not null.
     *
     * @throws NullPointerException
     *     If the input object to check is null.
     */
    @Contract("null, _ -> fail")
    public <T> T requireNonNull(@Nullable T obj, String name)
        throws NullPointerException
    {
        //noinspection ConstantConditions
        return Objects.requireNonNull(obj, name + " must not be null!");
    }

    /**
     * Logs a throwable using {@link PLogger#logThrowable(Throwable)} and returns a fallback value.
     * <p>
     * Mostly useful for {@link CompletableFuture#exceptionally(Function)}.
     *
     * @param throwable
     *     The throwable to send to the logger.
     * @param fallback
     *     The fallback value to return.
     * @param <T>
     *     The type of the fallback value.
     * @return The fallback value.
     */
    @Contract("_, !null -> !null")
    public @Nullable <T> T exceptionally(Throwable throwable, @Nullable T fallback)
    {
        BigDoors.get().getPLogger().logThrowable(throwable);
        return fallback;
    }

    /**
     * See {@link #exceptionally(Throwable, Object)} with a null fallback value.
     *
     * @return Always null
     */
    public @Nullable <T> T exceptionally(Throwable throwable)
    {
        return exceptionally(throwable, null);
    }

    /**
     * See {@link #exceptionally(Throwable, Object)} with a fallback value of {@link Optional#empty()}.
     *
     * @return Always {@link Optional#empty()}.
     */
    public <T> Optional<T> exceptionallyOptional(Throwable throwable)
    {
        return exceptionally(throwable, Optional.empty());
    }

    /**
     * Handles exceptional completion of a {@link CompletableFuture}. This ensure that the target is finished
     * exceptionally as well, to propagate the exception.
     *
     * @param throwable
     *     The {@link Throwable} to log.
     * @param fallback
     *     The fallback value to return.
     * @param target
     *     The {@link CompletableFuture} to complete.
     * @return The fallback value.
     */
    public <T, U> T exceptionallyCompletion(Throwable throwable, T fallback, CompletableFuture<U> target)
    {
        target.completeExceptionally(throwable);
        return fallback;
    }

    /**
     * Handles exceptional completion of a {@link CompletableFuture}. This ensure that the target is finished
     * exceptionally as well, to propagate the exception.
     *
     * @param throwable
     *     The {@link Throwable} to log.
     * @param target
     *     The {@link CompletableFuture} to complete.
     * @return Always null;
     */
    public <T> Void exceptionallyCompletion(Throwable throwable, CompletableFuture<T> target)
    {
        target.completeExceptionally(throwable);
        return null;
    }

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

    public static OptionalInt parseInt(Optional<String> str)
    {
        return str.map(Util::parseInt).orElse(OptionalInt.empty());
    }

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

    public static OptionalDouble parseDouble(Optional<String> str)
    {
        return str.map(Util::parseDouble).orElse(OptionalDouble.empty());
    }

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

    public static OptionalLong parseLong(Optional<String> str)
    {
        return str.map(Util::parseLong).orElse(OptionalLong.empty());
    }

    /**
     * See {@link #getDistanceToDoor(IPLocation, AbstractDoor)}.
     * <p>
     * If the player object has no location, -2 is returned.
     */
    public static double getDistanceToDoor(IPPlayer player, AbstractDoor door)
    {
        return player.getLocation().map(location -> getDistanceToDoor(location, door)).orElse(-2d);
    }

    /**
     * Gets the distance between a location and a door. If the location and the door are not in the same world, -1 is
     * returned.
     *
     * @param location
     *     The location to check.
     * @param door
     *     The door to check.
     * @return The distance between the location and the door if they lie in the same world, otherwise -1.
     */
    public static double getDistanceToDoor(IPLocation location, AbstractDoor door)
    {
        if (!location.getWorld().equals(door.getWorld()))
            return -1;
        return door.getCuboid().getCenter().getDistance(location.getPosition());
    }

    /**
     * Gets a {@link NotNull} value from a {@link Nullable} one, with a provided fallback in case the value is null.
     *
     * @param value
     *     The value that may or may not be null.
     * @param fallback
     *     A {@link Supplier} to supply a fallback to return in case the value is null.
     * @param <T>
     *     The type of the value.
     * @return The value if it is not null, otherwise the fallback.
     */
    public <T> T valOrDefault(@Nullable T value, Supplier<T> fallback)
    {
        return value == null ? fallback.get() : value;
    }

    /**
     * Searches through an {@link Iterable} object using a provided search function.
     *
     * @param iterable
     *     The {@link Iterable} object to search through.
     * @param searchPred
     *     The search predicate to use.
     * @param <T>
     *     The type of objects stored in the {@link Iterable}.
     * @return The value in the {@link Iterable} object for which the search function returns true, otherwise {@link
     * Optional#empty()}.
     */
    public <T> Optional<T> searchIterable(Iterable<T> iterable, Predicate<T> searchPred)
    {
        for (T val : iterable)
            if (searchPred.test(val))
                return Optional.of(val);
        return Optional.empty();
    }

    /**
     * Obtains the numbers of question marks in a String.
     *
     * @param statement
     *     The String.
     * @return The number of question marks in the String.
     */
    public static int countPatternOccurrences(Pattern pattern, String statement)
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
     * @param pBlockFace
     *     The {@link PBlockFace}.
     * @return The {@link RotateDirection} equivalent of a {@link PBlockFace} if it exists and otherwise {@link
     * RotateDirection#NONE}.
     */
    public static RotateDirection getRotateDirection(PBlockFace pBlockFace)
    {
        return TO_ROTATE_DIRECTION.getOrDefault(pBlockFace, RotateDirection.NONE);
    }

    /**
     * Gets the {@link PBlockFace} equivalent of a {@link RotateDirection} if it exists.
     *
     * @param rotateDirection
     *     The {@link RotateDirection}.
     * @return The {@link PBlockFace} equivalent of a {@link RotateDirection} if it exists and otherwise {@link
     * PBlockFace#NONE}.
     */
    public static PBlockFace getPBlockFace(RotateDirection rotateDirection)
    {
        return TO_PBLOCK_FACE.getOrDefault(rotateDirection, PBlockFace.NONE);
    }

    /**
     * Capitalizes the first letter. The rest of the String is left intact.
     *
     * @param string
     *     The string for which to capitalize the first letter.
     * @return The same string that it received as input, but with a capizalized first letter.
     */
    public static String capitalizeFirstLetter(String string)
    {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
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
        return angle % (2 * Math.PI);
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
        return angle % (2 * Math.PI);
    }

    /**
     * Concatenate two arrays.
     *
     * @param <T>
     * @param first
     *     First array.
     * @param second
     *     Second array.
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
     * @param arr
     *     Array to be doubled in size
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
     * @param arr
     *     The array to truncate
     * @param newLength
     *     The new length of the array.
     * @return A truncated array
     */
    public static <T> T[] truncateArray(T[] arr, int newLength)
    {
        return Arrays.copyOf(arr, newLength);
    }

    /**
     * Check if a given string is a valid door name. The following input is not allowed:
     * <p>
     * - Numerical names (numerical values are reserved for UIDs).
     * <p>
     * - Empty input.
     * <p>
     * - Input containing spaces.
     *
     * @param name
     *     The name to test for validity,
     * @return True if the name is allowed.
     */
    public static boolean isValidDoorName(@Nullable String name)
    {
        if (name == null || name.isBlank())
            return false;

        return Util.parseLong(name).isEmpty() && Util.parseDouble(name).isEmpty();
    }

    /**
     * Generate an insecure random alphanumeric string of a given length.
     *
     * @param length
     *     Length of the resulting string
     * @return An insecure random alphanumeric string.
     */
    public static String randomInsecureString(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for (int idx = 0; idx != length; ++idx)
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }

    /**
     * Generate a secure random alphanumeric string of a given length.
     *
     * @param length
     *     Length of the resulting string
     * @return A secure random alphanumeric string.
     */
    public static String secureRandomString(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for (int idx = 0; idx != length; ++idx)
            sb.append(CHARS.charAt(SECURE_RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }

    public static boolean hasPermissionForAction(UUID uuid, AbstractDoor door, DoorAttribute attribute)
    {
        return door.getDoorOwner(uuid)
                   .map(doorOwner -> doorOwner.permission() <= DoorAttribute.getPermissionLevel(attribute))
                   .orElse(false);
    }

    public static boolean hasPermissionForAction(IPPlayer player, AbstractDoor door, DoorAttribute attribute)
    {
        return hasPermissionForAction(player.getUUID(), door, attribute);
    }

    /**
     * Obtains a random integer value.
     *
     * @param min
     *     The lower bound (inclusive).
     * @param max
     *     The lower bound (inclusive).
     * @return A random integer value.
     */
    public static int getRandomNumber(int min, int max)
    {

        if (min >= max)
        {
            throw new IllegalArgumentException("max must be greater than min");
        }

        return RANDOM.nextInt((max - min) + 1) + min;
    }

    /**
     * Gets the chunk coordinates of a position.
     *
     * @param position
     *     The position.
     * @return The chunk coordinates.
     */
    public static Vector2Di getChunkCoords(Vector3Di position)
    {
        return new Vector2Di(position.x() << 4, position.z() << 4);
    }

    /**
     * Gets the 'simple' hash of the chunk given its coordinates. 'simple' here refers to the fact that the world of
     * this chunk will not be taken into account.
     *
     * @param chunkX
     *     The x-coordinate of the chunk.
     * @param chunkZ
     *     The z-coordinate of the chunk.
     * @return The simple hash of the chunk.
     */
    public static long simpleChunkHashFromChunkCoordinates(int chunkX, int chunkZ)
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
     * @param posX
     *     The x-coordinate of the location.
     * @param posZ
     *     The z-coordinate of the location.
     * @return The simple hash of the chunk.
     */
    public static long simpleChunkHashFromLocation(int posX, int posZ)
    {
        return simpleChunkHashFromChunkCoordinates(posX >> 4, posZ >> 4);
    }

    /**
     * Gets the 'simple' hash of a location. 'simple' here refers to the fact that the world of this location will not
     * be taken into account.
     *
     * @param x
     *     The x-coordinate of the location.
     * @param y
     *     The z-coordinate of the location.
     * @param z
     *     The z-coordinate of the location.
     * @return The simple hash of the location.
     */
    public static long simpleLocationhash(int x, int y, int z)
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
     * @param position
     *     The position in world space coordinates.
     * @return The coordinates in chunkspace coordinates.
     */
    public static Vector3Di getChunkSpacePosition(Vector3Di position)
    {
        return getChunkSpacePosition(position.x(), position.y(), position.z());
    }

    /**
     * Converts world space coordinates to chunk space coordinates.
     *
     * @param x
     *     The x coordinate in world space.
     * @param y
     *     The y coordinate in world space.
     * @param z
     *     The z coordinate in world space.
     * @return The coordinates in chunkspace coordinates.
     */
    public static Vector3Di getChunkSpacePosition(int x, int y, int z)
    {
        return new Vector3Di(x % 16, y, z % 16);
    }

    /**
     * Gets the 'simple' hash of a location in chunk-space. 'simple' here refers to the fact that the world of this
     * location will not be taken into account.
     *
     * @param x
     *     The x-coordinate of the location.
     * @param y
     *     The z-coordinate of the location.
     * @param z
     *     The z-coordinate of the location.
     * @return The simple hash of the location in chunk-space.
     */
    public static int simpleChunkSpaceLocationhash(int x, int y, int z)
    {
        int chunkSpaceX = x % 16;
        int chunkSpaceZ = z % 16;
        return (y << 8) + (chunkSpaceX << 4) + chunkSpaceZ;
    }

    /**
     * Convert a collection of objects into a single string.
     *
     * @param entries
     *     Input collection of objects.
     * @param mapper
     *     The function to map objects to strings.
     * @return Resulting concatenated string.
     */
    public static <T> String toString(T[] entries, Function<T, String> mapper)
    {
        return toString(Arrays.asList(entries), mapper);
    }

    /**
     * Convert a collection of objects into a single string.
     *
     * @param entries
     *     Input collection of objects.
     * @return Resulting concatenated string.
     */
    public static String toString(Object[] entries)
    {
        return toString(Arrays.asList(entries));
    }

    /**
     * Convert a collection of objects into a single string.
     *
     * @param entries
     *     Input collection of objects.
     * @return Resulting concatenated string.
     */
    public static String toString(Collection<?> entries)
    {
        return toString(entries, Object::toString);
    }

    /**
     * Convert a collection of objects into a single string.
     *
     * @param entries
     *     Input collection of objects.
     * @param mapper
     *     The function to map objects to strings.
     * @return Resulting concatenated string.
     */
    // NullAway doesn't appear to enjoy nullable values in an enhanced for-each loop.
    @SuppressWarnings("NullAway")
    public static <T> String toString(Collection<@Nullable T> entries, Function<T, String> mapper)
    {
        StringBuilder builder = new StringBuilder("[");
        for (@Nullable final T obj : entries)
            builder.append(obj == null ? "NULL" : mapper.apply(obj)).append(", ");

        String result = builder.toString();
        final int len = result.length();

        // If 1 or more entries exist, the output will end with ', ', so remove the last 2 characters in that case.
        return (len > 2 ? result.substring(0, len - 2) : result) + "]";
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

    @Deprecated
    public static int tickRateFromSpeed(double speed)
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
    public static double[] calculateTimeAndTickRate(int doorSize, double time, double speedMultiplier, double baseSpeed)
    {
        final double[] ret = new double[3];
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
