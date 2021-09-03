package nl.pim16aap2.bigdoors;

import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class UnitTestUtil
{
    @SuppressWarnings("unused")
    public static final double EPSILON = 1E-6;

    /**
     * Initializes and registers a new {@link IBigDoorsPlatform}. A {@link BasicPLogger} is also set up.
     *
     * @return The new {@link IBigDoorsPlatform}.
     */
    public static IBigDoorsPlatform initPlatform()
    {
        IBigDoorsPlatform platform = Mockito.mock(IBigDoorsPlatform.class);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());
        final var localizer = initLocalizer();
        Mockito.when(platform.getLocalizer()).thenReturn(localizer);
        return platform;
    }

    public static ILocalizer initLocalizer()
    {
        final var localizer = Mockito.mock(ILocalizer.class);
        Mockito.when(localizer.getMessage(Mockito.anyString()))
               .thenAnswer(invocation -> invocation.getArgument(0, String.class));
        Mockito.when(localizer.getMessage(Mockito.anyString(), Mockito.any()))
               .thenAnswer(invocation ->
                           {
                               String ret = invocation.getArgument(0, String.class);
                               if (invocation.getArguments().length == 1)
                                   return ret;

                               for (int idx = 1; idx < invocation.getArguments().length; ++idx)
                                   //noinspection StringConcatenationInLoop
                                   ret += " " + invocation.getArgument(idx, Object.class);
                               return ret;
                           });
        return localizer;
    }

    public static IPWorld getWorld()
    {
        final var world = Mockito.mock(IPWorld.class);
        Mockito.when(world.worldName()).thenReturn(UUID.randomUUID().toString());
        return world;
    }

    @SuppressWarnings("unused")
    public static IPLocation getLocation(Vector3Dd vec)
    {
        return getLocation(vec.x(), vec.y(), vec.z());
    }

    @SuppressWarnings("unused")
    public static IPLocation getLocation(Vector3Di vec)
    {
        return getLocation(vec.x(), vec.y(), vec.z());
    }

    @SuppressWarnings("unused")
    public static IPLocation getLocation(Vector3Dd vec, IPWorld world)
    {
        return getLocation(vec.x(), vec.y(), vec.z(), world);
    }

    public static IPLocation getLocation(Vector3Di vec, IPWorld world)
    {
        return getLocation(vec.x(), vec.y(), vec.z(), world);
    }

    public static IPLocation getLocation(double x, double y, double z)
    {
        return getLocation(x, y, z, getWorld());
    }

    public static IPLocation getLocation(double x, double y, double z, IPWorld world)
    {
        final var loc = Mockito.mock(IPLocation.class);

        Mockito.when(loc.getWorld()).thenReturn(world);

        Mockito.when(loc.getX()).thenReturn(x);
        Mockito.when(loc.getY()).thenReturn(y);
        Mockito.when(loc.getZ()).thenReturn(z);

        Mockito.when(loc.getBlockX()).thenReturn((int) x);
        Mockito.when(loc.getBlockY()).thenReturn((int) y);
        Mockito.when(loc.getBlockZ()).thenReturn((int) z);

        Mockito.when(loc.getPosition()).thenReturn(new Vector3Di((int) x, (int) y, (int) z));

        Mockito.when(loc.getChunk()).thenReturn(new Vector2Di(((int) x) << 4, ((int) z) << 4));

        return loc;
    }

    /**
     * Checks if an object and an Optional are the same or if they both don't exist/are null.
     *
     * @param obj
     *     The object to compare the optional to.
     * @param opt
     *     The Optional to compare against the object.
     * @param <T>
     *     The type of the Object and Optional.
     * @return The object inside the Optional.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static @Nullable <T> T optionalEquals(@Nullable T obj, Optional<T> opt)
    {
        if (obj == null)
        {
            Assertions.assertTrue(opt.isEmpty());
            return null;
        }
        Assertions.assertTrue(opt.isPresent());
        Assertions.assertEquals(obj, opt.get());
        return opt.get();
    }

    /**
     * Checks if an object and the mapped value of an Optional are the same or if they both don't exist/are null.
     *
     * @param obj
     *     The object to compare the optional to.
     * @param opt
     *     The Optional to compare against the object.
     * @param map
     *     The mapping function to apply to the value inside the optional (if that exists).
     * @param <T>
     *     The type of the Object and the result of the mapping functions.
     * @param <U>
     *     The type of the object stored inside the optional.
     * @return The object inside the Optional (so without the mapping function applied!).
     */
    @SuppressWarnings("UnusedReturnValue")
    public static @Nullable <T, U> U optionalEquals(@Nullable T obj, Optional<U> opt, Function<U, T> map)
    {
        if (obj == null)
        {
            Assertions.assertTrue(opt.isEmpty());
            return null;
        }
        Assertions.assertTrue(opt.isPresent());

        Assertions.assertEquals(obj, map.apply(opt.get()));
        return opt.get();
    }

    /**
     * Asserts a specific exception wrapped inside a {@link RuntimeException} is thrown by an {@link Executable}.
     *
     * @param expectedType
     *     The {@link Throwable} expected to be thrown wrapped inside a {@link RuntimeException}.
     * @param executable
     *     The {@link Executable} to execute that is expected to throw an exception.
     * @param <T>
     *     The type of the throwable wrapped inside the RuntimeException.
     */
    @SuppressWarnings("unused")
    public static <T extends Throwable> void assertWrappedThrows(Class<T> expectedType, Executable executable)
    {
        assertWrappedThrows(expectedType, executable, false);
    }

    /**
     * Asserts a specific exception wrapped inside a {@link RuntimeException} is thrown by an {@link Executable}.
     *
     * @param expectedType
     *     The {@link Throwable} expected to be thrown wrapped inside a {@link RuntimeException}.
     * @param executable
     *     The {@link Executable} to execute that is expected to throw an exception.
     * @param deepSearch
     *     Whether to keep digging through any number of layered {@link RuntimeException}s until we find a throwable
     *     that is not a RuntimeException.
     * @param <T>
     *     The type of the throwable wrapped inside the RuntimeException.
     */
    public static <T extends Throwable> void assertWrappedThrows(Class<T> expectedType, Executable executable,
                                                                 boolean deepSearch)
    {
        RuntimeException rte = Assertions.assertThrows(RuntimeException.class, executable);
        if (deepSearch)
            while (rte.getCause().getClass() == RuntimeException.class)
                rte = (RuntimeException) rte.getCause();
        Assertions.assertEquals(expectedType, rte.getCause().getClass(), expectedType.toString());
    }
}
