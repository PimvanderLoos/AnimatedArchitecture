package nl.pim16aap2.bigdoors.doors;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.util.FastFieldCopier;
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
    private final @NonNull List<Field> fields = new ArrayList<>();

    /**
     * The target class.
     */
    private final @NonNull Class<T> doorClass;

    /**
     * The constructor in the {@link #doorClass} that takes exactly 1 argument of the type {@link
     * AbstractDoorBase.DoorData} if such a constructor exists.
     */
    private final @Nullable Constructor<T> ctor;

    /**
     * The {@link Unsafe} instance.
     */
    private static final Unsafe UNSAFE;

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
    private static final Method INIT_METHOD;

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

    public DoorSerializer(final @NonNull Class<T> doorClass)
    {
        this.doorClass = doorClass;
        Constructor<T> ctor = null;
        try
        {
            ctor = doorClass.getDeclaredConstructor(AbstractDoorBase.DoorData.class);
            ctor.setAccessible(true);
        }
        catch (Throwable t)
        {
            // TODO: Enable this after fixing up the unit tests.
//            BigDoors.get().getPLogger()
//                   .logThrowable(Level.FINER, t, "Could not access required ctor for class: " + getDoorTypeName() +
//                       ". Defaulting to Unsafe!");
        }
        this.ctor = ctor;
        if (this.ctor == null && !UNSAFE_AVAILABLE)
        {
            IllegalStateException exception = new IllegalStateException(
                "Could not find CTOR for class " + getDoorTypeName() +
                    " and Unsafe is unavailable! This type cannot be enabled!");
            BigDoors.get().getPLogger().logThrowableSilently(exception);
            throw exception;
        }
        BigDoors.get().getPLogger().logMessage(Level.FINE, "Using " + (this.ctor == null ? "Unsafe" : "Reflection") +
            " construction method for class " + getDoorTypeName());

        findAnnotatedFields();
    }

    private void findAnnotatedFields()
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
                {
                    UnsupportedOperationException e = new UnsupportedOperationException(
                        String.format("Type %s of field %s for door type %s is not serializable!",
                                      field.getType().getName(), field.getName(), getDoorTypeName()));
                    BigDoors.get().getPLogger().logThrowableSilently(e);
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
    public byte[] serialize(final AbstractDoorBase door)
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
                BigDoors.get().getPLogger().logThrowableSilently(ex);
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
            BigDoors.get().getPLogger().logThrowableSilently(e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private static @NonNull ArrayList<Object> fromByteArray(final byte[] arr)
    {
        try (final ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(arr)))
        {
            final Object obj = objectInputStream.readObject();
            if (!(obj instanceof ArrayList))
            {
                IllegalStateException e = new IllegalStateException(
                    "Unexpected deserialization type! Expected ArrayList, but got " + obj.getClass().getName());
                BigDoors.get().getPLogger().logThrowableSilently(e);
                throw e;
            }
            return (ArrayList<Object>) obj;
        }
        catch (Throwable t)
        {
            RuntimeException e = new RuntimeException("Failed to deserialize object!", t);
            BigDoors.get().getPLogger().logThrowableSilently(e);
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
            BigDoors.get().getPLogger().logThrowableSilently(e);
            throw e;
        }

        try
        {
            T door = instantiate(doorData);
            if (door == null)
                throw new IllegalStateException("Failed to initialize door!");
            for (int idx = 0; idx < fields.size(); ++idx)
                fields.get(idx).set(door, values.get(idx));
            return door;
        }
        catch (Throwable t)
        {
            RuntimeException e = new RuntimeException("Failed to create new instance of type: " + getDoorTypeName(), t);
            BigDoors.get().getPLogger().logThrowableSilently(e);
            throw e;
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
    private @Nullable T instantiate(final @NonNull AbstractDoorBase.DoorData doorData)
        throws IllegalAccessException, InstantiationException, InvocationTargetException
    {
        return ctor != null ? instantiateReflection(doorData, ctor) : instantiateUnsafe(doorData);
    }

    private @NonNull T instantiateReflection(final @NonNull AbstractDoorBase.DoorData doorData,
                                             final @NonNull Constructor<T> ctor)
        throws IllegalAccessException, InvocationTargetException, InstantiationException
    {
        return ctor.newInstance(doorData);
    }

    private @Nullable T instantiateUnsafe(final @NonNull AbstractDoorBase.DoorData doorData)
        throws InstantiationException, IllegalAccessException, InvocationTargetException
    {
        if (!UNSAFE_AVAILABLE)
            return null;
        @SuppressWarnings("unchecked")
        T door = (T) UNSAFE.allocateInstance(doorClass);

        FIELD_COPIER_UID.copy(doorData, door);
        FIELD_COPIER_WORLD.copy(doorData, door);
        FIELD_COPIER_PRIME_OWNER.copy(doorData, door);
        FIELD_COPIER_DOOR_OWNERS.copy(doorData, door);

        INIT_METHOD.invoke(door, doorData);
        return door;
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
