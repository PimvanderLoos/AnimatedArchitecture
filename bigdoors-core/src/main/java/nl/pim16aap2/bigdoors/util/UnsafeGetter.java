package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Represents an accessor for {@link Unsafe}.
 *
 * @author Pim
 */
public class UnsafeGetter
{
    private static @Nullable Unsafe unsafe;
    private static volatile boolean unsafeInitialized = false;
    private static final Object lck = new Object();

    /**
     * Tries to retrieve {@link Unsafe}.
     *
     * @param logger
     *     The logger to log any exceptions to.
     * @return Unsafe if it could be found, otherwise null.
     */
    public static @Nullable Unsafe getUnsafe(IPLogger logger)
    {
        try
        {
            return unsafe;
        }
        catch (Exception e)
        {
            logger.logThrowable(e, "Failed to access unsafe!");
            return null;
        }
    }

    /**
     * Tries to retrieve {@link Unsafe}.
     *
     * @return Unsafe.
     *
     * @throws Exception
     *     When an exception occurred trying to access Unsafe.
     */
    public static Unsafe getUnsafe()
        throws Exception
    {
        return Util.requireNonNull(getUnsafe0(), "unsafe");
    }

    private static @Nullable Unsafe getUnsafe0()
        throws Exception
    {
        if (unsafeInitialized)
            return unsafe;

        synchronized (lck)
        {
            if (!unsafeInitialized)
                unsafe = newUnsafe();
            unsafeInitialized = true;
            return unsafe;
        }
    }

    private static @Nullable Unsafe newUnsafe()
        throws Exception
    {
        final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return (Unsafe) unsafeField.get(null);
    }

}
