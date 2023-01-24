package nl.pim16aap2.bigdoors.movable;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.util.FastFieldSetter;
import nl.pim16aap2.bigdoors.util.UnsafeGetter;
import nl.pim16aap2.util.SafeStringBuilder;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages the serialization aspects of the movables.
 *
 * @param <T>
 *     The type of movable.
 * @author Pim
 */
@Flogger
public class MovableSerializer<T extends AbstractMovable>
{
    /**
     * The list of serializable fields in the target class {@link #movableClass}.
     */
    private final List<Field> fields = new ArrayList<>();

    /**
     * The target class.
     */
    private final Class<T> movableClass;

    /**
     * The constructor in the {@link #movableClass} that takes exactly 1 argument of the type {@link MovableBase} if
     * such a constructor exists.
     */
    private final @Nullable Constructor<T> ctor;

    private static final @Nullable Unsafe UNSAFE = UnsafeGetter.getUnsafe();

    private final @Nullable FastFieldSetter<AbstractMovable, MovableBase> fieldSetterMovableBase =
        getFieldSetterInAbstractMovable(UNSAFE, MovableBase.class, "base");
    private final @Nullable FastFieldSetter<AbstractMovable, ReentrantReadWriteLock> fieldSetterLock =
        getFieldSetterInAbstractMovable(UNSAFE, ReentrantReadWriteLock.class, "lock");
    @SuppressWarnings("rawtypes")
    private final @Nullable FastFieldSetter<AbstractMovable, MovableSerializer> fieldSetterSerializer =
        getFieldSetterInAbstractMovable(UNSAFE, MovableSerializer.class, "serializer");

    public MovableSerializer(Class<T> movableClass)
    {
        this.movableClass = movableClass;

        if (Modifier.isAbstract(movableClass.getModifiers()))
            throw new IllegalArgumentException("THe MovableSerializer only works for concrete classes!");

        @Nullable Constructor<T> ctorTmp = null;
        try
        {
            ctorTmp = movableClass.getDeclaredConstructor(MovableBase.class);
            ctorTmp.setAccessible(true);
        }
        catch (Exception e)
        {
            log.atFiner().withCause(e)
               .log("Class %s does not have a MovableData ctor! Using Unsafe instead!", getMovableTypeName());
        }
        ctor = ctorTmp;
        if (ctor == null && UNSAFE == null)
            throw new RuntimeException("Could not find CTOR for class " + getMovableTypeName() +
                                           " and Unsafe is unavailable! This type cannot be enabled!");

        log.atFine().log("Using %s construction method for class %s.",
                         (ctor == null ? "Unsafe" : "Reflection"), getMovableTypeName());

        findAnnotatedFields();
    }

    private void findAnnotatedFields()
        throws UnsupportedOperationException
    {
        final List<Field> fieldList = new ArrayList<>();
        Class<?> clazz = movableClass;
        while (!clazz.equals(AbstractMovable.class))
        {
            try
            {
                fieldList.addAll(0, Arrays.asList(clazz.getDeclaredFields()));
            }
            catch (Throwable t)
            {
                log.atSevere().withCause(t).log("Failed to load class '%s'", clazz.getName());
                if (clazz.getName().endsWith("BigDoor"))
                    Runtime.getRuntime().exit(0);
            }
            clazz = clazz.getSuperclass();
        }

        for (final Field field : fieldList)
            if (field.isAnnotationPresent(PersistentVariable.class))
            {
                field.setAccessible(true);
                if (!field.getType().isPrimitive() && !Serializable.class.isAssignableFrom(field.getType()))
                    throw new UnsupportedOperationException(
                        String.format("Type %s of field %s for movable type %s is not serializable!",
                                      field.getType().getName(), field.getName(), getMovableTypeName()));
                fields.add(field);
            }
    }

    /**
     * Serializes the type-specific data of a movable.
     *
     * @param movable
     *     The movable.
     * @return The serialized type-specific data.
     */
    public byte[] serialize(AbstractMovable movable)
        throws Exception
    {
        final ArrayList<Object> values = new ArrayList<>(fields.size());
        for (final Field field : fields)
            try
            {
                values.add(field.get(movable));
            }
            catch (IllegalAccessException e)
            {
                throw new Exception(String.format("Failed to get value of field %s (type %s) for movable type %s!",
                                                  field.getName(), field.getType().getName(), getMovableTypeName()), e);
            }
        return toByteArray(values);
    }

    /**
     * Deserializes the serialized type-specific data of a movable.
     * <p>
     * The movable and the deserialized data are then used to create an instance of the movable type.
     *
     * @param movable
     *     The base movable data.
     * @param data
     *     The serialized type-specific data.
     * @return The newly created instance.
     */
    public T deserialize(MovableBase movable, byte[] data)
        throws Exception
    {
        return instantiate(movable, fromByteArray(data));
    }

    private static byte[] toByteArray(Serializable serializable)
        throws Exception
    {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream))
        {
            objectOutputStream.writeObject(serializable);
            return byteArrayOutputStream.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Object> fromByteArray(byte[] arr)
        throws Exception
    {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(arr)))
        {
            final Object obj = objectInputStream.readObject();
            if (!(obj instanceof ArrayList))
                throw new IllegalStateException(
                    "Unexpected deserialization type! Expected ArrayList, but got " + obj.getClass().getName());

            //noinspection unchecked
            return (ArrayList<Object>) obj;
        }
    }

    T instantiate(MovableBase movableBase, ArrayList<Object> values)
        throws Exception
    {
        if (values.size() != fields.size())
            throw new IllegalStateException(String.format("Expected %d arguments but received %d for type %s",
                                                          fields.size(), values.size(), getMovableTypeName()));

        try
        {
            final @Nullable T movable = instantiate(movableBase);
            if (movable == null)
                throw new IllegalStateException("Failed to initialize movable!");
            for (int idx = 0; idx < fields.size(); ++idx)
                fields.get(idx).set(movable, values.get(idx));
            return movable;
        }
        catch (Exception t)
        {
            throw new Exception("Failed to create new instance of type: " + getMovableTypeName(), t);
        }
    }

    /**
     * Attempts to create a new instance of {@link #movableClass} using the provided base data.
     * <p>
     * When {@link #ctor} is available, {@link #instantiateReflection(MovableBase, Constructor)} is used. If that is not
     * the case, {@link #instantiateUnsafe(MovableBase)} is used instead.
     *
     * @param movableBase
     *     The {@link MovableBase} to use for basic {@link AbstractMovable} initialization.
     * @return A new instance of {@link #movableClass} if one could be constructed.
     */
    private @Nullable T instantiate(MovableBase movableBase)
        throws IllegalAccessException, InstantiationException, InvocationTargetException
    {
        return ctor == null ? instantiateUnsafe(movableBase) : instantiateReflection(movableBase, ctor);
    }

    private T instantiateReflection(MovableBase movableBase, Constructor<T> ctor)
        throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        return ctor.newInstance(movableBase);
    }

    private @Nullable T instantiateUnsafe(MovableBase movableBase)
        throws InstantiationException
    {
        if (UNSAFE == null ||
            fieldSetterMovableBase == null ||
            fieldSetterLock == null ||
            fieldSetterSerializer == null)
            return null;

        @SuppressWarnings("unchecked") //
        final T movable = (T) UNSAFE.allocateInstance(movableClass);

        fieldSetterMovableBase.copy(movable, movableBase);
        fieldSetterLock.copy(movable, movableBase.getLock());
        fieldSetterSerializer.copy(movable, this);

        return movable;
    }

    public String getMovableTypeName()
    {
        return movableClass.getName();
    }

    /**
     * Prints the persistent field names and values of a movable.
     * <p>
     * 1 field per line.
     *
     * @param movable
     *     The {@link AbstractMovable} whose {@link PersistentVariable}s to print.
     * @return A String containing the names and values of the persistent parameters of the provided movable.
     */
    public String toString(AbstractMovable movable)
    {
        if (!movableClass.isAssignableFrom(movable.getClass()))
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Expected type '%s' but received type '%s'!", getMovableTypeName(), movable.getClass().getName());
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        for (final Field field : fields)
        {
            String value;
            try
            {
                value = field.get(movable).toString();
            }
            catch (IllegalAccessException e)
            {
                log.atSevere().withCause(e).log();
                value = "ERROR";
            }
            sb.append(field.getName()).append(": ").append(value).append('\n');
        }
        return sb.toString();
    }

    private String getConstructionModeName()
    {
        if (this.ctor == null && UNSAFE == null)
            return "No method available!";
        return this.ctor == null ? "Unsafe" : "Constructor";
    }

    @Override
    public String toString()
    {
        final SafeStringBuilder sb = new SafeStringBuilder("MovableSerializer: ")
            .append(getMovableTypeName()).append(", Construction Mode: ").append(getConstructionModeName())
            .append(", fields:\n");

        for (final Field field : fields)
            sb.append("* Type: ").append(field.getType().getName())
              .append(", name: \"").append(field.getName())
              .append("\"\n");
        return sb.toString();
    }

    private static @Nullable <T> FastFieldSetter<AbstractMovable, T> getFieldSetterInAbstractMovable(
        @Nullable Unsafe unsafe, Class<T> type, String fieldName)
    {
        if (unsafe == null)
            return null;
        try
        {
            return FastFieldSetter.of(unsafe, type, AbstractMovable.class, fieldName);
        }
        catch (Exception e)
        {
            log.atFine().withCause(e).log("Failed to get FastFieldSetter for %s AbstractMovableBase#%s",
                                          type.getName(), fieldName);
            return null;
        }
    }
}
