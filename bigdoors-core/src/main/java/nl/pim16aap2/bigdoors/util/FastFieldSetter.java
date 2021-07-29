package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.BigDoors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Uses {@link Unsafe} to quickly copy primitives and objects from one object to another.
 *
 * @param <T> The type of the target object.
 * @param <U> The type of the field to be set.
 * @author Pim
 */
public abstract class FastFieldSetter<T, U>
{
    /**
     * The {@link Unsafe} instance.
     */
    private static final @Nullable Unsafe UNSAFE;

    static
    {
        // Get the Unsafe instance.
        Unsafe unsafe = null;
        try
        {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(e);
        }
        UNSAFE = unsafe;
    }

    final long offsetTarget;

    private FastFieldSetter(long offsetTarget)
    {
        this.offsetTarget = offsetTarget;
    }

    private static Field getField(@NotNull Class<?> clz, @NotNull String name)
        throws NoSuchFieldException
    {
        Field f = clz.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /**
     * Copies the data from the source to the target from and to the previously defined fields.
     *
     * @param target The target object.
     * @param obj    The object to be set in the target object.
     */
    public abstract void copy(@NotNull T target, @NotNull U obj);

    /**
     * Creates a new {@link FastFieldSetter} for the provided fields.
     *
     * @param fieldType   The type of the field to be set.
     * @param targetClass The target class where the value that is to be copied is copied into.
     * @param nameTarget  The name of the target {@link Field}.
     * @param <T>         The type of the target class.
     * @return A new {@link FastFieldSetter} for the appropriate type.
     */
    public static @NotNull <T, U> FastFieldSetter<T, U> of(@NotNull Class<U> fieldType, @NotNull Class<T> targetClass,
                                                           @NotNull String nameTarget)
    {
        Util.requireNonNull(UNSAFE, "Unsafe");
        final long offsetTarget;
        final Class<?> targetType;
        try
        {
            Field fieldTarget = getField(targetClass, nameTarget);
            if (fieldTarget.getType() != fieldType)
                throw new IllegalArgumentException(
                    String.format("Target type %s does not match source type %s for target class %s",
                                  fieldTarget.getType().getName(), fieldType.getName(),
                                  targetClass.getName()));

            offsetTarget = UNSAFE.objectFieldOffset(fieldTarget);
            targetType = fieldTarget.getType();
        }
        catch (Throwable t)
        {
            RuntimeException e = new RuntimeException("Failed targetClass construct FastFieldCopier!", t);
            BigDoors.get().getPLogger().logThrowableSilently(e);
            throw e;
        }

        // All these methods suppress NullAway, because it complains about UNSAFE, but it should
        // never even get to this point if UNSAFE is null.
        if (targetType.equals(int.class))
            return new FastFieldSetter<>(offsetTarget)
            {
                @Override
                @SuppressWarnings("NullAway")
                public void copy(@NotNull Object target, @NotNull Object obj)
                {
                    UNSAFE.putInt(target, offsetTarget, (int) obj);
                }
            };

        if (targetType.equals(long.class))
            return new FastFieldSetter<>(offsetTarget)
            {
                @Override
                @SuppressWarnings("NullAway")
                public void copy(@NotNull Object target, @NotNull Object obj)
                {
                    UNSAFE.putLong(target, offsetTarget, (long) obj);
                }
            };

        if (targetType.equals(boolean.class))
            return new FastFieldSetter<>(offsetTarget)
            {
                @Override
                @SuppressWarnings("NullAway")
                public void copy(@NotNull Object target, @NotNull Object obj)
                {
                    UNSAFE.putBoolean(target, offsetTarget, (boolean) obj);
                }
            };

        if (targetType.equals(short.class))
            return new FastFieldSetter<>(offsetTarget)
            {
                @Override
                @SuppressWarnings("NullAway")
                public void copy(@NotNull Object target, @NotNull Object obj)
                {
                    UNSAFE.putShort(target, offsetTarget, (short) obj);
                }
            };

        if (targetType.equals(char.class))
            return new FastFieldSetter<>(offsetTarget)
            {
                @Override
                @SuppressWarnings("NullAway")
                public void copy(@NotNull Object target, @NotNull Object obj)
                {
                    UNSAFE.putChar(target, offsetTarget, (char) obj);
                }
            };

        if (targetType.equals(float.class))
            return new FastFieldSetter<>(offsetTarget)
            {
                @Override
                @SuppressWarnings("NullAway")
                public void copy(@NotNull Object target, @NotNull Object obj)
                {
                    UNSAFE.putFloat(target, offsetTarget, (float) obj);
                }
            };

        if (targetType.equals(double.class))
            return new FastFieldSetter<>(offsetTarget)
            {
                @Override
                @SuppressWarnings("NullAway")
                public void copy(@NotNull Object target, @NotNull Object obj)
                {
                    UNSAFE.putDouble(target, offsetTarget, (double) obj);
                }
            };

        if (targetType.equals(byte.class))
            return new FastFieldSetter<>(offsetTarget)
            {
                @Override @SuppressWarnings("NullAway")
                public void copy(@NotNull Object target, @NotNull Object obj)
                {
                    UNSAFE.putByte(target, offsetTarget, (byte) obj);
                }
            };

        return new FastFieldSetter<>(offsetTarget)
        {
            @Override @SuppressWarnings("NullAway")
            public void copy(@NotNull Object target, @NotNull Object obj)
            {
                UNSAFE.putObject(target, offsetTarget, obj);
            }
        };
    }
}
