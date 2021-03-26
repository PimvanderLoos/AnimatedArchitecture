package nl.pim16aap2.bigdoors.util;

import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Uses {@link Unsafe} to quickly copy primitives and objects from one object to another.
 *
 * @param <S> The type of the source object.
 * @param <T> The type of the target object.
 * @author Pim
 */
public abstract class FastFieldCopier<S, T>
{
    /**
     * The {@link Unsafe} instance.
     */
    private static final Unsafe UNSAFE;

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
            PLogger.get().logThrowable(e);
        }
        UNSAFE = unsafe;
    }

    final long offsetSource;
    final long offsetTarget;

    private FastFieldCopier(long offsetSource, long offsetTarget)
    {
        this.offsetSource = offsetSource;
        this.offsetTarget = offsetTarget;
    }

    private static Field getField(@NonNull Class<?> clz, @NonNull String name)
        throws NoSuchFieldException
    {
        Field f = clz.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /**
     * Copies the data from the source to the target from and to the previously defined fields.
     *
     * @param source The source object.
     * @param target The target object.
     */
    public abstract void copy(@NotNull S source, @NotNull T target);

    /**
     * Creates a new {@link FastFieldCopier} for the provided fields.
     *
     * @param sourceClass The source class where the value that is to be copied is copied from.
     * @param nameSource  The name of the source {@link Field}.
     * @param targetClass The target class where the value that is to be copied is copied into.
     * @param nameTarget  The name of the target {@link Field}.
     * @param <S>         The type of the source class.
     * @param <T>         The type of the target class.
     * @return A new {@link FastFieldCopier} for the appropriate type.
     */
    public static @NonNull <S, T> FastFieldCopier<S, T> of(@NonNull Class<S> sourceClass, @NonNull String nameSource,
                                                           @NonNull Class<T> targetClass, @NonNull String nameTarget)
    {
        final long offsetSource;
        final long offsetTarget;
        final Class<?> targetType;
        try
        {
            Field fieldSource = getField(sourceClass, nameSource);
            Field fieldTarget = getField(targetClass, nameTarget);
            if (fieldTarget.getType() != fieldSource.getType())
                throw new IllegalArgumentException(
                    String.format("Target type %s does not match source type %s for target class %s",
                                  fieldTarget.getType().getName(), fieldSource.getType().getName(),
                                  targetClass.getName()));

            offsetSource = UNSAFE.objectFieldOffset(fieldSource);
            offsetTarget = UNSAFE.objectFieldOffset(fieldTarget);
            targetType = fieldTarget.getType();
        }
        catch (Throwable t)
        {
            RuntimeException e = new RuntimeException("Failed targetClass construct FastFieldCopier!", t);
            PLogger.get().logThrowableSilently(e);
            throw e;
        }

        if (targetType.equals(int.class))
            return new FastFieldCopier<>(offsetSource, offsetTarget)
            {
                @Override
                public void copy(@NotNull Object source, @NotNull Object target)
                {
                    UNSAFE.putInt(target, offsetTarget, UNSAFE.getInt(source, offsetSource));
                }
            };

        if (targetType.equals(long.class))
            return new FastFieldCopier<>(offsetSource, offsetTarget)
            {
                @Override
                public void copy(@NotNull Object source, @NotNull Object target)
                {
                    UNSAFE.putLong(target, offsetTarget, UNSAFE.getLong(source, offsetSource));
                }
            };

        if (targetType.equals(boolean.class))
            return new FastFieldCopier<>(offsetSource, offsetTarget)
            {
                @Override
                public void copy(@NotNull Object source, @NotNull Object target)
                {
                    UNSAFE.putBoolean(target, offsetTarget, UNSAFE.getBoolean(source, offsetSource));
                }
            };

        if (targetType.equals(short.class))
            return new FastFieldCopier<>(offsetSource, offsetTarget)
            {
                @Override
                public void copy(@NotNull Object source, @NotNull Object target)
                {
                    UNSAFE.putShort(target, offsetTarget, UNSAFE.getShort(source, offsetSource));
                }
            };

        if (targetType.equals(char.class))
            return new FastFieldCopier<>(offsetSource, offsetTarget)
            {
                @Override
                public void copy(@NotNull Object source, @NotNull Object target)
                {
                    UNSAFE.putChar(target, offsetTarget, UNSAFE.getChar(source, offsetSource));
                }
            };

        if (targetType.equals(float.class))
            return new FastFieldCopier<>(offsetSource, offsetTarget)
            {
                @Override
                public void copy(@NotNull Object source, @NotNull Object target)
                {
                    UNSAFE.putFloat(target, offsetTarget, UNSAFE.getFloat(source, offsetSource));
                }
            };

        if (targetType.equals(double.class))
            return new FastFieldCopier<>(offsetSource, offsetTarget)
            {
                @Override
                public void copy(@NotNull Object source, @NotNull Object target)
                {
                    UNSAFE.putDouble(target, offsetTarget, UNSAFE.getDouble(source, offsetSource));
                }
            };

        if (targetType.equals(byte.class))
            return new FastFieldCopier<>(offsetSource, offsetTarget)
            {
                @Override
                public void copy(@NotNull Object source, @NotNull Object target)
                {
                    UNSAFE.putByte(target, offsetTarget, UNSAFE.getByte(source, offsetSource));
                }
            };

        return new FastFieldCopier<>(offsetSource, offsetTarget)
        {
            @Override
            public void copy(@NotNull Object source, @NotNull Object target)
            {
                UNSAFE.putObject(target, offsetTarget, UNSAFE.getObject(source, offsetSource));
            }
        };
    }
}
