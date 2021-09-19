package nl.pim16aap2.bigdoors.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Uses {@link Unsafe} to quickly copy primitives and objects from one object to another.
 *
 * @param <T>
 *     The type of the target object.
 * @param <U>
 *     The type of the field to be set.
 * @author Pim
 */
public abstract class FastFieldSetter<T, U>
{
    protected FastFieldSetter()
    {
    }

    private static Field getField(Class<?> clz, String name)
        throws NoSuchFieldException
    {
        final Field f = clz.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    /**
     * Copies the data from the source to the target from and to the previously defined fields.
     *
     * @param target
     *     The target object.
     * @param obj
     *     The object to be set in the target object.
     */
    public abstract void copy(T target, U obj);

    /**
     * Creates a new {@link FastFieldSetter} for the provided fields.
     *
     * @param fieldType
     *     The type of the field to be set.
     * @param targetClass
     *     The target class where the value that is to be copied is copied into.
     * @param nameTarget
     *     The name of the target {@link Field}.
     * @param <T>
     *     The type of the target class.
     * @return A new {@link FastFieldSetter} for the appropriate type.
     */

    public static <T, U> FastFieldSetter<T, U> of(Unsafe unsafe,
                                                  Class<U> fieldType, Class<T> targetClass, String nameTarget)
        throws Exception
    {
        final long offsetTarget;
        final Class<?> targetType;
        final Field fieldTarget;
        try
        {
            fieldTarget = getField(targetClass, nameTarget);
        }
        catch (NoSuchFieldException e)
        {
            throw new Exception("Failed to find method \"" + nameTarget +
                                    "\" in target class: " + targetClass.getName(), e);
        }

        if (fieldTarget.getType() != fieldType)
            throw new IllegalArgumentException(
                String.format("Target type %s does not match source type %s for target class %s",
                              fieldTarget.getType().getName(), fieldType.getName(),
                              targetClass.getName()));

        offsetTarget = unsafe.objectFieldOffset(fieldTarget);
        targetType = fieldTarget.getType();

        // All these methods suppress NullAway, because it complains about UNSAFE, but it should
        // never even get to this point if UNSAFE is null.
        if (targetType.equals(int.class))
            return new FastFieldSetter<>()
            {
                @Override
                public void copy(Object target, Object obj)
                {
                    unsafe.putInt(target, offsetTarget, (int) obj);
                }
            };

        if (targetType.equals(long.class))
            return new FastFieldSetter<>()
            {
                @Override
                public void copy(Object target, Object obj)
                {
                    unsafe.putLong(target, offsetTarget, (long) obj);
                }
            };

        if (targetType.equals(boolean.class))
            return new FastFieldSetter<>()
            {
                @Override
                public void copy(Object target, Object obj)
                {
                    unsafe.putBoolean(target, offsetTarget, (boolean) obj);
                }
            };

        if (targetType.equals(short.class))
            return new FastFieldSetter<>()
            {
                @Override
                public void copy(Object target, Object obj)
                {
                    unsafe.putShort(target, offsetTarget, (short) obj);
                }
            };

        if (targetType.equals(char.class))
            return new FastFieldSetter<>()
            {
                @Override
                public void copy(Object target, Object obj)
                {
                    unsafe.putChar(target, offsetTarget, (char) obj);
                }
            };

        if (targetType.equals(float.class))
            return new FastFieldSetter<>()
            {
                @Override
                public void copy(Object target, Object obj)
                {
                    unsafe.putFloat(target, offsetTarget, (float) obj);
                }
            };

        if (targetType.equals(double.class))
            return new FastFieldSetter<>()
            {
                @Override
                public void copy(Object target, Object obj)
                {
                    unsafe.putDouble(target, offsetTarget, (double) obj);
                }
            };

        if (targetType.equals(byte.class))
            return new FastFieldSetter<>()
            {
                @Override
                public void copy(Object target, Object obj)
                {
                    unsafe.putByte(target, offsetTarget, (byte) obj);
                }
            };

        return new FastFieldSetter<>()
        {
            @Override
            public void copy(Object target, Object obj)
            {
                unsafe.putObject(target, offsetTarget, obj);
            }
        };
    }

    /**
     * See {@link #of(Unsafe, Class, Class, String)}.
     *
     * @throws Exception
     *     When an exception was thrown while trying to access Unsafe.
     */
    public static <T, U> FastFieldSetter<T, U> of(Class<U> fieldType, Class<T> targetClass, String nameTarget)
        throws Exception
    {
        final Unsafe unsafe = UnsafeGetter.getRequiredUnsafe();
        return of(unsafe, fieldType, targetClass, nameTarget);
    }
}
