package nl.pim16aap2.bigdoors.util;

import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Represents an accessor for {@link Unsafe}.
 *
 * @author Pim
 */
@Flogger
public final class UnsafeGetter
{
    private static @Nullable Unsafe unsafe;
    private static volatile boolean unsafeInitialized = false;
    private static final Object LCK = new Object();

    private UnsafeGetter()
    {
        // Utility class
    }

    /**
     * Tries to retrieve {@link Unsafe}.
     *
     * @return Unsafe if it could be found, otherwise null.
     */
    public static @Nullable Unsafe getUnsafe()
    {
        try
        {
            return getUnsafe0();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to access unsafe!");
            return null;
        }
    }

    /**
     * Tries to retrieve {@link Unsafe} and throws an exception when it is not available.
     *
     * @return Unsafe.
     *
     * @throws Exception
     *     When an exception occurred trying to access Unsafe.
     */
    public static Unsafe getRequiredUnsafe()
        throws Exception
    {
        return Util.requireNonNull(getUnsafe0(), "unsafe");
    }

    private static @Nullable Unsafe getUnsafe0()
        throws Exception
    {
        if (unsafeInitialized)
            return unsafe;

        synchronized (LCK)
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
