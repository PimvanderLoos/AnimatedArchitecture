package nl.pim16aap2.bigdoors;

import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class UnitTestUtil
{
    public static final double EPSILON = 1E-6;

    /**
     * Initializes and registers a new {@link IBigDoorsPlatform}. A {@link BasicPLogger} is also set up.
     *
     * @return The new {@link IBigDoorsPlatform}.
     */
    public static @NotNull IBigDoorsPlatform initPlatform()
    {
        IBigDoorsPlatform platform = Mockito.mock(IBigDoorsPlatform.class);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());
        return platform;
    }

    public static @NotNull Messages initMessages()
    {
        val messages = Mockito.mock(Messages.class);
        Mockito.when(messages.getString(Mockito.any()))
               .thenAnswer(invocation -> invocation.getArgument(0, Message.class).name());
        Mockito.when(messages.getString(Mockito.any(), Mockito.any()))
               .thenAnswer(invocation ->
                           {
                               String ret = invocation.getArgument(0, Message.class).name();
                               if (invocation.getArguments().length == 1)
                                   return ret;

                               for (int idx = 1; idx < invocation.getArguments().length; ++idx)
                                   //noinspection StringConcatenationInLoop
                                   ret += " " + invocation.getArgument(idx, String.class);
                               return ret;
                           });
        return messages;
    }

    public static @NotNull IPWorld getWorld()
    {
        val world = Mockito.mock(IPWorld.class);
        Mockito.when(world.getWorldName()).thenReturn(UUID.randomUUID().toString());
        return world;
    }

    public static @NotNull IPLocation getLocation(final @NotNull Vector3DdConst vec)
    {
        return getLocation(vec.getX(), vec.getY(), vec.getZ());
    }

    public static @NotNull IPLocation getLocation(final @NotNull Vector3DiConst vec)
    {
        return getLocation(vec.getX(), vec.getY(), vec.getZ());
    }

    public static @NotNull IPLocation getLocation(final @NotNull Vector3DdConst vec, final @NotNull IPWorld world)
    {
        return getLocation(vec.getX(), vec.getY(), vec.getZ(), world);
    }

    public static @NotNull IPLocation getLocation(final @NotNull Vector3DiConst vec, final @NotNull IPWorld world)
    {
        return getLocation(vec.getX(), vec.getY(), vec.getZ(), world);
    }

    public static @NotNull IPLocation getLocation(final double x, final double y, final double z)
    {
        return getLocation(x, y, z, getWorld());
    }

    public static @NotNull IPLocation getLocation(final double x, final double y, final double z,
                                                  final @NotNull IPWorld world)
    {
        val loc = Mockito.mock(IPLocation.class);

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
     * @param obj The object to compare the optional to.
     * @param opt The Optional to compare against the object.
     * @param <T> The type of the Object and Optional.
     * @return The object inside the Optional.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static <T> T optionalEquals(final @Nullable T obj, final @NotNull Optional<T> opt)
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
     * @param obj The object to compare the optional to.
     * @param opt The Optional to compare against the object.
     * @param map The mapping function to apply to the value inside the optional (if that exists).
     * @param <T> The type of the Object and the result of the mapping functions.
     * @param <U> The type of the object stored inside the optional.
     * @return The object inside the Optional (so without the mapping function applied!).
     */
    @SuppressWarnings("UnusedReturnValue")
    public static <T, U> U optionalEquals(final @Nullable T obj, final @NotNull Optional<U> opt,
                                          @NotNull Function<U, T> map)
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
     * @param expectedType The {@link Throwable} expected to be thrown wrapped inside a {@link RuntimeException}.
     * @param executable   The {@link Executable} to execute that is expected to throw an exception.
     * @param <T>          The type of the throwable wrapped inside the RuntimeException.
     */
    @SuppressWarnings("unused")
    public static <T extends Throwable> void assertWrappedThrows(Class<T> expectedType, Executable executable)
    {
        assertWrappedThrows(expectedType, executable, false);
    }

    /**
     * Asserts a specific exception wrapped inside a {@link RuntimeException} is thrown by an {@link Executable}.
     *
     * @param expectedType The {@link Throwable} expected to be thrown wrapped inside a {@link RuntimeException}.
     * @param executable   The {@link Executable} to execute that is expected to throw an exception.
     * @param deepSearch   Whether to keep digging through any number of layered {@link RuntimeException}s until we find
     *                     a throwable that is not a RuntimeException.
     * @param <T>          The type of the throwable wrapped inside the RuntimeException.
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
