package nl.pim16aap2.bigdoors.movable;

import com.google.common.flogger.StackSize;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.movable.serialization.DeserializationConstructor;
import nl.pim16aap2.bigdoors.movable.serialization.PersistentVariable;
import nl.pim16aap2.reflection.ReflectionBuilder;
import nl.pim16aap2.util.SafeStringBuilder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

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
     * The target class.
     */
    private final Class<T> movableClass;
    /**
     * The list of serializable fields in the target class {@link #movableClass} that are annotated with
     * {@link PersistentVariable}.
     */
    private final List<AnnotatedField> fields;

    /**
     * The constructor in the {@link #movableClass} that takes exactly 1 argument of the type {@link MovableBase}.
     */
    private final Constructor<T> ctor;

    /**
     * The parameters of the {@link #ctor}.
     */
    private final List<ConstructorParameter> parameters;

    public MovableSerializer(Class<T> movableClass)
    {
        this.movableClass = movableClass;

        if (Modifier.isAbstract(movableClass.getModifiers()))
            throw new IllegalArgumentException("THe MovableSerializer only works for concrete classes!");

        fields = findAnnotatedFields(movableClass);
        ctor = getConstructor(movableClass);
        parameters = getConstructorParameters(ctor);
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
            if (parameter.getType() == AbstractMovable.MovableBaseHolder.class && !foundBase)
            {
                foundBase = true;
                ret.add(new ConstructorParameter("", AbstractMovable.MovableBaseHolder.class));
                continue;
            }

            final ConstructorParameter constructorParameter = ConstructorParameter.of(parameter);
            if (constructorParameter.isUnnamed() && !unnamedParameters.add(parameter.getType()))
                throw new IllegalArgumentException(
                    "Found ambiguous parameter " + parameter + " in constructor: " + ctor);

            ret.add(constructorParameter);
        }

        if (!foundBase)
            throw new IllegalArgumentException(
                "Could not found parameter MovableBaseHolder in deserialization constructor: " + ctor);

        return ret;
    }

    private static List<AnnotatedField> findAnnotatedFields(Class<? extends AbstractMovable> movableClass)
        throws UnsupportedOperationException
    {
        return ReflectionBuilder
            .findField().inClass(movableClass)
            .withAnnotations(PersistentVariable.class)
            .checkSuperClasses()
            .setAccessible()
            .get().stream()
            .map(AnnotatedField::of)
            .toList();
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
            log.atWarning().log("Expected %d arguments but received %d for type %s",
                                fields.size(), values.size(), getMovableTypeName());
        @Nullable Object @Nullable [] deserializedParameters = null;
        try
        {
            deserializedParameters = deserializeParameters(movableBase, values);
            return ctor.newInstance(deserializedParameters);
        }
        catch (Exception t)
        {
            throw new Exception(
                "Failed to create new instance of type: " + getMovableTypeName() + ", with parameters: " +
                    Arrays.toString(deserializedParameters), t);
        }
    }

    private Object[] deserializeParameters(AbstractMovable.MovableBaseHolder base, Map<String, Object> values)
    {
        final Map<Class<?>, Object> classes = new HashMap<>(values.size());
        for (final var entry : values.entrySet())
            classes.put(entry.getValue().getClass(), entry.getValue());

        final Object[] ret = new Object[this.parameters.size()];
        int idx = -1;
        for (final ConstructorParameter param : this.parameters)
        {
            ++idx;

            try
            {
                final @Nullable Object data;
                if (param.type == AbstractMovable.MovableBaseHolder.class)
                    data = base;
                else if (param.name != null)
                    data = getDeserializedObject(base, values, param.name);
                else
                    data = getDeserializedObject(base, classes, param.type);

                if (param.isRemappedFromPrimitive && data == null)
                    throw new IllegalArgumentException(
                        "Received null parameter that cannot accept null values: " + param);

                //noinspection DataFlowIssue
                ret[idx] = data;
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException(
                    String.format("Could not set index %d in constructor from key %s from values %s.",
                                  idx, (param.isUnnamed() ? param.type : param.name),
                                  (param.isUnnamed() ? classes : values)), e);
            }
        }
        return ret;
    }

    private static @Nullable <T> Object getDeserializedObject(
        AbstractMovable.MovableBaseHolder base, Map<T, Object> map, T key)
    {
        final @Nullable Object ret = map.get(key);
        if (ret != null)
            return ret;

        if (!map.containsKey(key))
            log.atSevere().log("No value found for key '%s' for movable: %s", key, base);

        return null;
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
        public static AnnotatedField of(Field field)
        {
            verifyFieldType(field);
            final String annotatedName = field.getAnnotation(PersistentVariable.class).value();
            final String fieldName = field.getName();
            final String finalName = annotatedName.isBlank() ? fieldName : annotatedName;
            final @Nullable String finalAnnotatedName = annotatedName.isBlank() ? null : annotatedName;
            return new AnnotatedField(field, finalAnnotatedName, fieldName, finalName);
        }

        private static void verifyFieldType(Field field)
        {
            if (!field.getType().isPrimitive() && !Serializable.class.isAssignableFrom(field.getType()))
                throw new UnsupportedOperationException(
                    String.format("Type %s of field %s is not serializable!",
                                  field.getType().getName(), field.getName()));
        }

        public String typeName()
        {
            return field.getType().getName();
        }
    }

    private record ConstructorParameter(@Nullable String name, Class<?> type, boolean isRemappedFromPrimitive)
    {
        public ConstructorParameter(@Nullable String name, Class<?> type)
        {
            this(name, remapPrimitives(type), type.isPrimitive());
        }

        public static ConstructorParameter of(Parameter parameter)
        {
            return new ConstructorParameter(getName(parameter), parameter.getType());
        }

        private static @Nullable String getName(Parameter parameter)
        {
            final @Nullable var annotation = parameter.getAnnotation(PersistentVariable.class);
            //noinspection ConstantValue
            if (annotation == null)
                return null;
            return annotation.value().isBlank() ? null : annotation.value();
        }

        boolean isUnnamed()
        {
            return name == null;
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
    }
}
