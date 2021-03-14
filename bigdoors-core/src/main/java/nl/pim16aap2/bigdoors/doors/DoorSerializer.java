package nl.pim16aap2.bigdoors.doors;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.util.PLogger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the serialization aspects of the doors.
 *
 * @param <T> The type of door.
 * @author Pim
 */
public class DoorSerializer<T extends AbstractDoorBase>
{
    private final @NonNull List<Field> fields = new ArrayList<>();
    private final @NonNull Class<T> doorClass;
    private final @NonNull Constructor<T> ctor;

    public DoorSerializer(final @NonNull Class<T> doorClass)
    {
        this.doorClass = doorClass;
        try
        {
            ctor = doorClass.getConstructor(AbstractDoorBase.DoorData.class);
            findAnnotatedFields();
        }
        catch (Throwable t)
        {
            final RuntimeException e = new RuntimeException("Failed to analyze door type: " + getDoorTypeName(), t);
            PLogger.get().logThrowableSilently(e);
            throw e;
        }
    }

    private void findAnnotatedFields()
    {
        for (final Field field : doorClass.getDeclaredFields())
            if (field.isAnnotationPresent(PersistentVariable.class))
            {
                field.setAccessible(true);
                if (!field.getType().isPrimitive() && !Serializable.class.isAssignableFrom(field.getType()))
                {
                    UnsupportedOperationException e = new UnsupportedOperationException(
                        String.format("Type %s of field %s for door type %s is not serializable!",
                                      field.getType().getName(), field.getName(), getDoorTypeName()));
                    PLogger.get().logThrowableSilently(e);
                    throw e;
                }
                fields.add(field);
            }
    }

    /**
     * Serializes the type-specific data of a door.
     *
     * @param door The door.
     * @return The serialized type-specific data.
     */
    public byte[] serialize(final @NonNull T door)
    {
        final ArrayList<Object> values = new ArrayList<>(fields.size());
        for (final Field field : fields)
            try
            {
                values.add(field.get(door));
            }
            catch (IllegalAccessException e)
            {
                RuntimeException ex = new RuntimeException(
                    String.format("Failed to get value of field %s (type %s) for door type %s!",
                                  field.getName(), field.getType().getName(), getDoorTypeName()), e);
                PLogger.get().logThrowableSilently(ex);
                throw ex;
            }
        return toByteArray(values);
    }

    /**
     * Deserializes the serialized type-specific data of a door.
     * <p>
     * The base doorData and the deserialized data is then used to create a instance of the door type.
     *
     * @param doorData The base door data.
     * @param data     The serialized type-specific data.
     * @return The newly created instance.
     */
    public T deserialize(final @NonNull AbstractDoorBase.DoorData doorData, final byte[] data)
    {
        return instantiate(doorData, fromByteArray(data));
    }

    private static byte[] toByteArray(final @NonNull Serializable serializable)
    {
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream))
        {
            objectOutputStream.writeObject(serializable);
            return byteArrayOutputStream.toByteArray();
        }
        catch (Throwable t)
        {
            RuntimeException e = new RuntimeException("Failed to serialize object: " + serializable.toString(), t);
            PLogger.get().logThrowableSilently(e);
            throw e;
        }
    }

    private static @NonNull ArrayList<Object> fromByteArray(final byte[] arr)
    {
        try (final ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(arr)))
        {
            final Object obj = objectInputStream.readObject();
            if (!(obj instanceof ArrayList))
            {
                IllegalStateException e = new IllegalStateException(
                    "Unexpected deserialization type! Expected ArrayList, but got " + obj.getClass().getName());
                PLogger.get().logThrowableSilently(e);
                throw e;
            }
            return (ArrayList<Object>) obj;
        }
        catch (Throwable t)
        {
            RuntimeException e = new RuntimeException("Failed to deserialize object!", t);
            PLogger.get().logThrowableSilently(e);
            throw e;
        }

    }

    @NonNull T instantiate(final @NonNull AbstractDoorBase.DoorData doorData,
                           final @NonNull ArrayList<Object> values)
    {
        if (values.size() != fields.size())
        {
            IllegalStateException e =
                new IllegalStateException(String.format("Expected %d arguments but received %d for type %s",
                                                        fields.size(), values.size(), getDoorTypeName()));
            PLogger.get().logThrowableSilently(e);
            throw e;
        }


        try
        {
            T door = ctor.newInstance(doorData);
            for (int idx = 0; idx < fields.size(); ++idx)
                fields.get(idx).set(door, values.get(idx));
            return door;
        }
        catch (Throwable t)
        {
            RuntimeException e = new RuntimeException("Failed to create new instance of type: " + getDoorTypeName(), t);
            PLogger.get().logThrowableSilently(e);
            throw e;
        }
    }

    public @NonNull String getDoorTypeName()
    {
        return doorClass.getName();
    }
}
