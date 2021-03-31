package nl.pim16aap2.bigdoors.doors;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
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
        throws Exception
    {
        this.doorClass = doorClass;
        try
        {
            ctor = doorClass.getDeclaredConstructor(AbstractDoorBase.DoorData.class);
            ctor.setAccessible(true);
            findAnnotatedFields();
        }
        catch (Throwable t)
        {
            throw new Exception("Failed to analyze door type: " + getDoorTypeName(), t);
        }
    }

    private void findAnnotatedFields()
        throws UnsupportedOperationException
    {
        List<Field> fieldList = new ArrayList<>();
        Class<?> clazz = doorClass;
        while (!clazz.equals(AbstractDoorBase.class))
        {
            fieldList.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }

        for (final Field field : fieldList)
            if (field.isAnnotationPresent(PersistentVariable.class))
            {
                field.setAccessible(true);
                if (!field.getType().isPrimitive() && !Serializable.class.isAssignableFrom(field.getType()))
                    throw new UnsupportedOperationException(
                        String.format("Type %s of field %s for door type %s is not serializable!",
                                      field.getType().getName(), field.getName(), getDoorTypeName()));
                fields.add(field);
            }
    }

    /**
     * Serializes the type-specific data of a door.
     *
     * @param door The door.
     * @return The serialized type-specific data.
     */
    public byte[] serialize(final AbstractDoorBase door)
        throws Exception
    {
        final ArrayList<Object> values = new ArrayList<>(fields.size());
        for (final Field field : fields)
            try
            {
                values.add(field.get(door));
            }
            catch (IllegalAccessException e)
            {
                throw new Exception(
                    String.format("Failed to get value of field %s (type %s) for door type %s!",
                                  field.getName(), field.getType().getName(), getDoorTypeName()), e);
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
        throws Exception
    {
        return instantiate(doorData, fromByteArray(data));
    }

    private static byte[] toByteArray(final @NonNull Serializable serializable)
        throws Exception
    {
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream))
        {
            objectOutputStream.writeObject(serializable);
            return byteArrayOutputStream.toByteArray();
        }
        catch (Throwable t)
        {
            throw new Exception(t);
        }
    }

    private static @NonNull ArrayList<Object> fromByteArray(final byte[] arr)
        throws Exception
    {
        try (final ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(arr)))
        {
            final Object obj = objectInputStream.readObject();
            if (!(obj instanceof ArrayList))
            {
                throw new IllegalStateException(
                    "Unexpected deserialization type! Expected ArrayList, but got " + obj.getClass().getName());
            }
            //noinspection unchecked
            return (ArrayList<Object>) obj;
        }
        catch (Throwable t)
        {
            throw new Exception(t);
        }
    }

    @NonNull T instantiate(final @NonNull AbstractDoorBase.DoorData doorData,
                           final @NonNull ArrayList<Object> values)
        throws Exception
    {
        if (values.size() != fields.size())
        {
            throw new IllegalStateException(String.format("Expected %d arguments but received %d for type %s",
                                                          fields.size(), values.size(), getDoorTypeName()));
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
            throw new Exception("Failed to create new instance of type: " + getDoorTypeName(), t);
        }
    }

    public @NonNull String getDoorTypeName()
    {
        return doorClass.getName();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("DoorSerializer for type: ").append(getDoorTypeName()).append("\n");
        for (Field field : fields)
            sb.append("Type: ").append(field.getType().getName())
              .append(", name: ").append(field.getName())
              .append("\n");
        return sb.toString();
    }
}
