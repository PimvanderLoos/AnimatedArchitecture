package nl.pim16aap2.bigdoors.movable;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.InheritedLockField;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.reflection.ReflectionBuilder;
import nl.pim16aap2.util.SafeStringBuilder;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
     * The list of serializable fields in the target class {@link #movableClass} that are annotated with
     * {@link PersistentVariable}.
     */
    private final List<Field> fields = new ArrayList<>();

    /**
     * The list of {@link ReentrantReadWriteLock} fields annotated with {@link InheritedLockField} in the target class
     * {@link #movableClass}.
     */
    private final List<Field> lockFields = new ArrayList<>();

    /**
     * The target class.
     */
    private final Class<T> movableClass;

    /**
     * The constructor in the {@link #movableClass} that takes exactly 1 argument of the type {@link MovableBase}.
     */
    private final Constructor<T> ctor;

    public MovableSerializer(Class<T> movableClass)
    {
        this.movableClass = movableClass;

        if (Modifier.isAbstract(movableClass.getModifiers()))
            throw new IllegalArgumentException("THe MovableSerializer only works for concrete classes!");

        //noinspection unchecked
        ctor = (Constructor<T>)
            ReflectionBuilder.findConstructor().inClass(movableClass)
                             .withParameters(AbstractMovable.MovableBaseHolder.class)
                             .setAccessible().get();

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
            else if (field.isAnnotationPresent(InheritedLockField.class))
            {
                field.setAccessible(true);
                if (field.getType() != ReentrantReadWriteLock.class)
                    throw new UnsupportedOperationException(
                        String.format(
                            "Field %s for movable type %s is of type %s, but expected ReentrantReadWriteLock!",
                            field.getName(), getMovableTypeName(), field.getType().getName()));
                lockFields.add(field);
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
    public T deserialize(AbstractMovable.MovableBaseHolder movable, byte[] data)
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

    @VisibleForTesting
    T instantiate(AbstractMovable.MovableBaseHolder movableBase, ArrayList<Object> values)
        throws Exception
    {
        if (values.size() != fields.size())
            throw new IllegalStateException(String.format("Expected %d arguments but received %d for type %s",
                                                          fields.size(), values.size(), getMovableTypeName()));

        try
        {
            final T movable = ctor.newInstance(movableBase);
            for (int idx = 0; idx < fields.size(); ++idx)
                fields.get(idx).set(movable, values.get(idx));

            final ReentrantReadWriteLock lock = movableBase.get().getLock();
            for (final Field field : lockFields)
                field.set(movable, lock);

            return movable;
        }
        catch (Exception t)
        {
            throw new Exception("Failed to create new instance of type: " + getMovableTypeName(), t);
        }
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

    @Override
    public String toString()
    {
        final SafeStringBuilder sb = new SafeStringBuilder("MovableSerializer: ")
            .append(getMovableTypeName())
            .append(", fields:\n");

        for (final Field field : fields)
            sb.append("* Type: ").append(field.getType().getName())
              .append(", name: \"").append(field.getName())
              .append("\"\n");
        return sb.toString();
    }
}
