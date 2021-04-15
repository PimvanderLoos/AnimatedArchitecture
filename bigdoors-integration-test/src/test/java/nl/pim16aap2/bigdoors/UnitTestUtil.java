package nl.pim16aap2.bigdoors;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

import java.util.Optional;
import java.util.function.Function;

/**
 * Class with utility methods to make unit tests easier.
 * <p>
 * This class does not test anything on its own.
 *
 * @author Pim
 */
@UtilityClass
public class UnitTestUtil
{
    public final double EPSILON = 1E-6;

    /**
     * Checks if an object and an Optional are the same or if they both don't exist/are null.
     *
     * @param obj The object to compare the optional to.
     * @param opt The Optional to compare against the object.
     * @param <T> The type of the Object and Optional.
     * @return The object inside the Optional.
     */
    @SuppressWarnings("UnusedReturnValue")
    public <T> T optionalEquals(final @Nullable T obj, final @NonNull Optional<T> opt)
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
    public <T, U> U optionalEquals(final @Nullable T obj, final @NonNull Optional<U> opt, @NonNull Function<U, T> map)
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
    public <T extends Throwable> void assertWrappedThrows(Class<T> expectedType, Executable executable)
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
    public <T extends Throwable> void assertWrappedThrows(Class<T> expectedType, Executable executable,
                                                          boolean deepSearch)
    {
        RuntimeException rte = Assertions.assertThrows(RuntimeException.class, executable);
        if (deepSearch)
            while (rte.getCause().getClass() == RuntimeException.class)
                rte = (RuntimeException) rte.getCause();
        Assertions.assertEquals(expectedType, rte.getCause().getClass(), expectedType.toString());
    }
}
