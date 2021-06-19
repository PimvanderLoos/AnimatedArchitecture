package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.util.FastFieldCopier;
import org.jetbrains.annotations.NotNull;
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

/**
 * Manages the serialization aspects of the doors.
 *
 * @param <T> The type of door.
 * @author Pim
 */
public class DoorSerializer<T extends AbstractDoorBase>
{
    /**
     * The list of serializable fields in the target class {@link #doorClass}.
     */
    private final @NotNull List<Field> fields = new ArrayList<>();

    /**
     * The target class.
     */
    private final @NotNull Class<T> doorClass;

    /**
     * The constructor in the {@link #doorClass} that takes exactly 1 argument of the type {@link
     * AbstractDoorBase.DoorData} if such a constructor exists.
     */
    private final @Nullable Constructor<T> ctor;

    /**
     * The {@link Unsafe} instance.
     */
    private static final @Nullable Unsafe UNSAFE;

    private static final FastFieldCopier<AbstractDoorBase.SimpleDoorData, AbstractDoorBase> FIELD_COPIER_UID =
        FastFieldCopier.of(AbstractDoorBase.SimpleDoorData.class, "uid", AbstractDoorBase.class, "doorUID");
    private static final FastFieldCopier<AbstractDoorBase.SimpleDoorData, AbstractDoorBase> FIELD_COPIER_WORLD =
        FastFieldCopier.of(AbstractDoorBase.SimpleDoorData.class, "world", AbstractDoorBase.class, "world");
    private static final FastFieldCopier<AbstractDoorBase.SimpleDoorData, AbstractDoorBase> FIELD_COPIER_PRIME_OWNER =
        FastFieldCopier.of(AbstractDoorBase.SimpleDoorData.class, "primeOwner", AbstractDoorBase.class, "primeOwner");
    private static final FastFieldCopier<AbstractDoorBase.DoorData, AbstractDoorBase> FIELD_COPIER_DOOR_OWNERS =
        FastFieldCopier.of(AbstractDoorBase.DoorData.class, "doorOwners", AbstractDoorBase.class, "doorOwners");

    /**
     * The init method in the {@link AbstractDoorBase} class.
     * <p>
     * Used to initialize all the non-final parameters when instantiating the {@link #doorClass} via the {@link #UNSAFE}
     * method.
     * <p>
     * See {@link #instantiateUnsafe(AbstractDoorBase.DoorData)}.
     */
    private static final @Nullable Method INIT_METHOD;

    /**
     * Checks if the unsafe method is available.
     * <p>
     * For this to be true both {@link #UNSAFE} and {@link #INIT_METHOD} must be available.
     */
    private static final boolean UNSAFE_AVAILABLE;

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

        Method initMethod = null;
        try
        {
            initMethod = AbstractDoorBase.class.getDeclaredMethod("init", AbstractDoorBase.DoorData.class);
            initMethod.setAccessible(true);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(e);
        }

        INIT_METHOD = initMethod;
        UNSAFE_AVAILABLE = UNSAFE != null && INIT_METHOD != null;
    }

    public DoorSerializer(final @NotNull Class<T> doorClass)
        throws Exception
    {
        this.doorClass = doorClass;
        if (Modifier.isAbstract(doorClass.getModifiers()))
            throw new IllegalArgumentException("THe DoorSerializer only works for concrete classes!");

        Constructor<T> ctorTmp = null;
        try
        {
            ctorTmp = doorClass.getDeclaredConstructor(AbstractDoorBase.DoorData.class);
            ctorTmp.setAccessible(true);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(Level.FINER, e, "Class " + getDoorTypeName() +
                " does not have DoorData ctor! Using Unsafe instead!");
        }
        ctor = ctorTmp;
        if (ctor == null && !UNSAFE_AVAILABLE)
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
        while (!clazz.equals(AbstractDoorBase.class))
        {
            fieldList.addAll(0, Arrays.asList(clazz.getDeclaredFields()));
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
                throw new Exception(String.format("Failed to get value of field %s (type %s) for door type %s!",
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
    public T deserialize(final @NotNull AbstractDoorBase.DoorData doorData, final byte[] data)
        throws Exception
    {
        return instantiate(doorData, fromByteArray(data));
    }

    private static byte[] toByteArray(final @NotNull Serializable serializable)
        throws Exception
    {
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream))
        {
            objectOutputStream.writeObject(serializable);
            return byteArrayOutputStream.toByteArray();
        }
    }

    @SuppressWarnings("unchecked")
    private static @NotNull ArrayList<Object> fromByteArray(final byte[] arr)
        throws Exception
    {
        try (final ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(arr)))
        {
            final Object obj = objectInputStream.readObject();
            if (!(obj instanceof ArrayList))
                throw new IllegalStateException(
                    "Unexpected deserialization type! Expected ArrayList, but got " + obj.getClass().getName());

            //noinspection unchecked
            return (ArrayList<Object>) obj;
        }
    }

    @NotNull T instantiate(final @NotNull AbstractDoorBase.DoorData doorData,
                           final @NotNull ArrayList<Object> values)
        throws Exception
    {
        if (values.size() != fields.size())
            throw new IllegalStateException(String.format("Expected %d arguments but received %d for type %s",
                                                          fields.size(), values.size(), getDoorTypeName()));

        try
        {
            @Nullable T door = instantiate(doorData);
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
     * When {@link #ctor} is available, {@link #instantiateReflection(AbstractDoorBase.DoorData, Constructor)} is used.
     * If that is not the case, {@link #instantiateUnsafe(AbstractDoorBase.DoorData)} is used instead.
     *
     * @param doorData The {@link AbstractDoorBase.DoorData} to use for basic {@link AbstractDoorBase} initialization.
     * @return A new instance of {@link #doorClass} if one could be constructed.
     */
    private @Nullable T instantiate(final @NotNull AbstractDoorBase.DoorData doorData)
        throws IllegalAccessException, InstantiationException, InvocationTargetException
    {
        return ctor != null ? instantiateReflection(doorData, ctor) : instantiateUnsafe(doorData);
    }

    private @NotNull T instantiateReflection(final @NotNull AbstractDoorBase.DoorData doorData,
                                             final @NotNull Constructor<T> ctor)
        throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        return ctor.newInstance(doorData);
    }

    @SuppressWarnings({"unchecked", "NullAway", "ConstantConditions"})
    private @Nullable T instantiateUnsafe(final @NotNull AbstractDoorBase.DoorData doorData)
        throws InstantiationException, IllegalAccessException, InvocationTargetException
    {
        if (!UNSAFE_AVAILABLE)
            return null;

        T door = (T) UNSAFE.allocateInstance(doorClass);

        FIELD_COPIER_UID.copy(doorData, door);
        FIELD_COPIER_WORLD.copy(doorData, door);
        FIELD_COPIER_PRIME_OWNER.copy(doorData, door);
        FIELD_COPIER_DOOR_OWNERS.copy(doorData, door);

        INIT_METHOD.invoke(door, doorData);
        return door;
    }

    public @NotNull String getDoorTypeName()
    {
        return doorClass.getName();
    }

    /**
     * Prints the persistent field names and values of a door.
     * <p>
     * 1 field per line.
     *
     * @param door The {@link AbstractDoorBase} whose {@link PersistentVariable}s to print.
     * @return A String containing the names and values of the persistent parameters of the provided door.
     */
    public String toString(@NotNull AbstractDoorBase door)
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
