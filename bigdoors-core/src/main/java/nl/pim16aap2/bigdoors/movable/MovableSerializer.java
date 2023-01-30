package nl.pim16aap2.bigdoors.movable;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.movable.serialization.DeserializationConstructor;
import nl.pim16aap2.bigdoors.movable.serialization.PersistentVariable;
import nl.pim16aap2.reflection.ReflectionBuilder;
import nl.pim16aap2.util.SafeStringBuilder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.inject.Named;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Manages the serialization aspects of the movables.
 *
 * @param <T>
 *     The type of movable.
 * @author Pim
 */
@Flogger
public final class MovableSerializer<T extends AbstractMovable>
{
    /**
     * The list of serializable fields in the target class {@link #movableClass} that are annotated with
     * {@link PersistentVariable}.
     */
    private final List<AnnotatedField> fields;

    private final List<ConstructorParameter> parameters;

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

        ctor = getConstructor(movableClass);
        parameters = getConstructorParameters(ctor);
        fields = findAnnotatedFields(movableClass);
    }

    private static <T> Constructor<T> getConstructor(Class<T> movableClass)
    {
        @SuppressWarnings("unchecked") //
        final Constructor<T> ctor = (Constructor<T>) ReflectionBuilder
            .findConstructor(movableClass)
            .withAnnotations(DeserializationConstructor.class)
            .setAccessible().get();
        return ctor;
    }

    private static List<ConstructorParameter> getConstructorParameters(Constructor<?> ctor)
    {
        final List<ConstructorParameter> ret = new ArrayList<>(ctor.getParameterCount());
        boolean foundBase = false;

        final Set<Class<?>> unnamedParameters = new HashSet<>();
        for (final Parameter parameter : ctor.getParameters())
        {
            if (parameter.getType() == AbstractMovable.MovableBaseHolder.class)
                foundBase = true;

            final @Nullable Named annotation = parameter.getAnnotation(Named.class);
            @SuppressWarnings("ConstantValue") // Yes, the annotation can actually be null...
            final @Nullable String name =
                annotation == null || annotation.value().isBlank() ? null : annotation.value();

            if (name == null && !unnamedParameters.add(parameter.getType()))
                throw new IllegalArgumentException(
                    "Found ambiguous parameter " + parameter + " in constructor: " + ctor);

            ret.add(new ConstructorParameter(name, parameter.getType()));
        }

        if (!foundBase)
            throw new IllegalArgumentException(
                "Could not found parameter MovableBaseHolder in deserialization constructor: " + ctor);

        return ret;
    }

    private static List<AnnotatedField> findAnnotatedFields(Class<? extends AbstractMovable> movableClass)
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
            }
            clazz = clazz.getSuperclass();
        }

        final List<AnnotatedField> ret = new ArrayList<>();
        for (final Field field : fieldList)
        {
            final @Nullable PersistentVariable annotation = field.getAnnotation(PersistentVariable.class);
            //noinspection ConstantValue
            if (annotation == null)
                continue;

            field.setAccessible(true);
            if (!field.getType().isPrimitive() && !Serializable.class.isAssignableFrom(field.getType()))
                throw new UnsupportedOperationException(
                    String.format("Type %s of field %s for movable type %s is not serializable!",
                                  field.getType().getName(), field.getName(), movableClass.getName()));

            ret.add(AnnotatedField.of(field, Objects.requireNonNull(annotation.value())));
        }
        return ret;
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
        final LinkedHashMap<String, Object> values = new LinkedHashMap<>(fields.size());
        for (final AnnotatedField field : fields)
            try
            {
                values.put(field.finalName, field.field.get(movable));
            }
            catch (IllegalAccessException e)
            {
                throw new Exception(String.format("Failed to get value of field %s (type %s) for movable type %s!",
                                                  field.fieldName(), field.typeName(), getMovableTypeName()), e);
            }
        return toByteArray(values);
    }

    /**
     * Deserializes the serialized type-specific data of a movable.
     * <p>
     * The movable and the deserialized data are then used to create an instance of the movable type.
     *
     * @param registry
     *     The registry to use for any potential registration.
     * @param movable
     *     The base movable data.
     * @param data
     *     The serialized type-specific data.
     * @return The newly created instance.
     */
    public T deserialize(MovableRegistry registry, AbstractMovable.MovableBaseHolder movable, byte[] data)
    {
        //noinspection unchecked
        return (T) registry.computeIfAbsent(movable.get().getUid(), () -> deserialize(movable, data));
    }

    @VisibleForTesting
    T deserialize(AbstractMovable.MovableBaseHolder movable, byte[] data)
    {
        @Nullable Map<String, Object> dataAsMap = null;
        try
        {
            dataAsMap = fromByteArray(data);
            return instantiate(movable, dataAsMap);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to deserialize movable " + movable + "\nWith Data: " + dataAsMap, e);
        }
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

    private static Map<String, Object> fromByteArray(byte[] arr)
        throws Exception
    {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(arr)))
        {
            final Object obj = objectInputStream.readObject();
            if (!(obj instanceof LinkedHashMap))
                throw new IllegalStateException(
                    "Unexpected deserialization type! Expected ArrayList, but got " + obj.getClass().getName());

            //noinspection unchecked
            return (Map<String, Object>) obj;
        }
    }

    @VisibleForTesting
    T instantiate(AbstractMovable.MovableBaseHolder movableBase, Map<String, Object> values)
        throws Exception
    {
        if (values.size() != fields.size())
            throw new IllegalStateException(
                String.format("Expected %d arguments but received %d for type %s",
                              fields.size(), values.size(), getMovableTypeName()));
        try
        {
            final Object[] deserializedParameters = deserializeParameters(movableBase, values);
            return ctor.newInstance(deserializedParameters);
        }
        catch (Exception t)
        {
            throw new Exception("Failed to create new instance of type: " + getMovableTypeName(), t);
        }
    }

    private Object[] deserializeParameters(
        AbstractMovable.MovableBaseHolder movableBase, Map<String, Object> values)
    {
        final Map<Class<?>, Object> classes = new HashMap<>(values.size());
        for (final var entry : values.entrySet())
            classes.put(entry.getValue().getClass(), entry.getValue());

        final Object[] ret = new Object[values.size() + 1];
        int idx = -1;
        for (final ConstructorParameter param : this.parameters)
        {
            ++idx;

            if (param.type == AbstractMovable.MovableBaseHolder.class)
                ret[idx] = movableBase;
            else if (param.hasName())
                ret[idx] = values.get(param.name);
            else
                ret[idx] = classes.get(param.type);
        }
        return ret;
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
        for (final AnnotatedField field : fields)
        {
            String value;
            try
            {
                value = field.field.get(movable).toString();
            }
            catch (IllegalAccessException e)
            {
                log.atSevere().withCause(e).log();
                value = "ERROR";
            }
            sb.append(field.field.getName()).append(": ").append(value).append('\n');
        }
        return sb.toString();
    }

    @Override
    public String toString()
    {
        final SafeStringBuilder sb = new SafeStringBuilder("MovableSerializer: ")
            .append(getMovableTypeName())
            .append(", fields:\n");

        for (final AnnotatedField field : fields)
            sb.append("* Type: ").append(field.typeName())
              .append(", name: \"").append(field.finalName)
              .append("\" (\"").append(field.annotatedName == null ? "unspecified" : field.annotatedName)
              .append("\")\n");
        return sb.toString();
    }

    private record AnnotatedField(Field field, @Nullable String annotatedName, String fieldName, String finalName)
    {
        public static AnnotatedField of(Field field, @Nullable String annotatedName)
        {
            final String annotatedName0 = Objects.requireNonNullElse(annotatedName, "");
            final String fieldName = field.getName();
            final String finalName = annotatedName0.isBlank() ? fieldName : annotatedName0;
            final @Nullable String finalAnnotatedName = annotatedName0.isBlank() ? null : annotatedName0;
            return new AnnotatedField(field, finalAnnotatedName, fieldName, finalName);
        }

        public String typeName()
        {
            return field.getType().getName();
        }
    }

    private record ConstructorParameter(@Nullable String name, Class<?> type)
    {
        public ConstructorParameter(@Nullable String name, Class<?> type)
        {
            this.name = name;
            this.type = remapPrimitives(type);
        }

        private static Class<?> remapPrimitives(Class<?> clz)
        {
            if (!clz.isPrimitive())
                return clz;
            if (clz == boolean.class)
                return Boolean.class;
            if (clz == char.class)
                return Character.class;
            if (clz == byte.class)
                return Byte.class;
            if (clz == short.class)
                return Short.class;
            if (clz == int.class)
                return Integer.class;
            if (clz == long.class)
                return Long.class;
            if (clz == float.class)
                return Float.class;
            if (clz == double.class)
                return Double.class;
            throw new IllegalStateException("Processing unexpected class type: " + clz);
        }

        boolean hasName()
        {
            return name != null;
        }
    }
}
