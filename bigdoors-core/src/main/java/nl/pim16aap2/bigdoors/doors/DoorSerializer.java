package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.util.FastFieldSetter;
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
import java.util.logging.Level;

/**
 * Manages the serialization aspects of the doors.
 *
 * @param <T>
 *     The type of door.
 * @author Pim
 */
public class DoorSerializer<T extends AbstractDoor>
{
    /**
     * The list of serializable fields in the target class {@link #doorClass}.
     */
    private final List<Field> fields = new ArrayList<>();

    /**
     * The target class.
     */
    private final Class<T> doorClass;

    /**
     * The constructor in the {@link #doorClass} that takes exactly 1 argument of the type {@link DoorBase} if such a
     * constructor exists.
     */
    private final @Nullable Constructor<T> ctor;

    private static final FastFieldSetter<AbstractDoor, DoorBase> FIELD_COPIER_DOOR_BASE =
        FastFieldSetter.of(DoorBase.class, AbstractDoor.class, "doorBase");

    /**
     * The {@link Unsafe} instance.
     */
    private static final @Nullable Unsafe UNSAFE;

    static
    {
        // Get the Unsafe instance.
        @Nullable Unsafe unsafe = null;
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

    public DoorSerializer(Class<T> doorClass)
        throws Exception
    {
        this.doorClass = doorClass;
        if (Modifier.isAbstract(doorClass.getModifiers()))
            throw new IllegalArgumentException("THe DoorSerializer only works for concrete classes!");

        @Nullable Constructor<T> ctorTmp = null;
        try
        {
            ctorTmp = doorClass.getDeclaredConstructor(DoorBase.class);
            ctorTmp.setAccessible(true);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(Level.FINER, e, "Class " + getDoorTypeName() +
                " does not have DoorData ctor! Using Unsafe instead!");
        }
        ctor = ctorTmp;
        if (ctor == null && UNSAFE == null)
            throw new Exception("Could not find CTOR for class " + getDoorTypeName() +
                                    " and Unsafe is unavailable! This type cannot be enabled!");

        BigDoors.get().getPLogger().logMessage(Level.FINE, "Using " + (ctor == null ? "Unsafe" : "Reflection") +
            " construction method for class " + getDoorTypeName());

        findAnnotatedFields();
    }

    private void findAnnotatedFields()
        throws UnsupportedOperationException
    {
        List<Field> fieldList = new ArrayList<>();
        Class<?> clazz = doorClass;
        while (!clazz.equals(AbstractDoor.class))
        {
            fieldList.addAll(0, Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }

        for (Field field : fieldList)
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
     * @param door
     *     The door.
     * @return The serialized type-specific data.
     */
    public byte[] serialize(AbstractDoor door)
        throws Exception
    {
        final ArrayList<Object> values = new ArrayList<>(fields.size());
        for (Field field : fields)
            try
            {
                values.add(field.get(door));
            }
            catch (IllegalAccessException e)
            {
                throw new Exception(String.format("Failed to get value of field %s (type %s) for door type %s!",
                                                  field.getName(), field.getType().getName(), getDoorTypeName()), e);
            }
        return toByteArray(values);
    }

    /**
     * Deserializes the serialized type-specific data of a door.
     * <p>
     * The doorBase and the deserialized data are then used to create an instance of the door type.
     *
     * @param doorBase
     *     The base door data.
     * @param data
     *     The serialized type-specific data.
     * @return The newly created instance.
     */
    public T deserialize(DoorBase doorBase, byte[] data)
        throws Exception
    {
        return instantiate(doorBase, fromByteArray(data));
    }

    private static byte[] toByteArray(Serializable serializable)
        throws Exception
    {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream))
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

    T instantiate(DoorBase doorBase, ArrayList<Object> values)
        throws Exception
    {
        if (values.size() != fields.size())
            throw new IllegalStateException(String.format("Expected %d arguments but received %d for type %s",
                                                          fields.size(), values.size(), getDoorTypeName()));

        try
        {
            @Nullable T door = instantiate(doorBase);
            if (door == null)
                throw new IllegalStateException("Failed to initialize door!");
            for (int idx = 0; idx < fields.size(); ++idx)
                fields.get(idx).set(door, values.get(idx));
            return door;
        }
        catch (Exception t)
        {
            throw new Exception("Failed to create new instance of type: " + getDoorTypeName(), t);
        }
    }

    /**
     * Attempts to create a new instance of {@link #doorClass} using the provided base data.
     * <p>
     * When {@link #ctor} is available, {@link #instantiateReflection(DoorBase, Constructor)} is used. If that is not
     * the case, {@link #instantiateUnsafe(DoorBase)} is used instead.
     *
     * @param doorBase
     *     The {@link DoorBase} to use for basic {@link AbstractDoor} initialization.
     * @return A new instance of {@link #doorClass} if one could be constructed.
     */
    private @Nullable T instantiate(DoorBase doorBase)
        throws IllegalAccessException, InstantiationException, InvocationTargetException
    {
        return ctor != null ? instantiateReflection(doorBase, ctor) : instantiateUnsafe(doorBase);
    }

    private T instantiateReflection(DoorBase doorBase, Constructor<T> ctor)
        throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        return ctor.newInstance(doorBase);
    }

    private @Nullable T instantiateUnsafe(DoorBase doorBase)
        throws InstantiationException
    {
        if (UNSAFE == null)
            return null;

        @SuppressWarnings("unchecked") //
        final T door = (T) UNSAFE.allocateInstance(doorClass);
        FIELD_COPIER_DOOR_BASE.copy(door, doorBase);
        return door;
    }

    public String getDoorTypeName()
    {
        return doorClass.getName();
    }

    /**
     * Prints the persistent field names and values of a door.
     * <p>
     * 1 field per line.
     *
     * @param door
     *     The {@link AbstractDoor} whose {@link PersistentVariable}s to print.
     * @return A String containing the names and values of the persistent parameters of the provided door.
     */
    public String toString(AbstractDoor door)
    {
        if (!doorClass.isAssignableFrom(door.getClass()))
        {
            BigDoors.get().getPLogger().logThrowable(new IllegalArgumentException(
                "Expected type " + getDoorTypeName() + " but received type " + door.getClass().getName()));
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Field field : fields)
        {
            String value;
            try
            {
                value = field.get(door).toString();
            }
            catch (IllegalAccessException e)
            {
                BigDoors.get().getPLogger().logThrowable(e);
                value = "ERROR";
            }
            sb.append(field.getName()).append(": ").append(value).append("\n");
        }
        return sb.toString();
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
