package nl.pim16aap2.bigdoors.movable;

import com.alibaba.fastjson2.JSON;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.movable.serialization.Deserialization;
import nl.pim16aap2.bigdoors.movable.serialization.PersistentVariable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.reflection.ReflectionBuilder;
import nl.pim16aap2.util.SafeStringBuilder;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Manages the serialization aspects of the movables.
 * <p>
 * The {@link PersistentVariable} annotation is used on fields to determine which fields are serialized. If a name is
 * provided to the annotation, the object will be serialized using that name. If more than one unnamed field of the same
 * type is defined, the serializer will throw an exception on startup. Similarly, there can be no two fields with the
 * same name.
 * <p>
 * In the constructor, the {@link PersistentVariable} annotation can be used to specify the name of the object to
 * deserialize. If no name is provided, the object is matched using its type instead. Like with the variables, no
 * ambiguity in parameter types or names is allowed.
 * <p>
 * The {@link AbstractMovable.MovableBaseHolder} object is always provided and does not need to be handled in any
 * specific way.
 * <p>
 * When a value is missing during deserialization, null will be substituted in its place if it is not a primitive. If
 * the type is a primitive, an exception will be thrown.
 * <p>
 * For example:
 * <pre> {@code
 * public class MyMovable extends AbstractMovable
 * {
 *     @PersistentVariable("ambiguousInteger0")
 *     private int myInt0;
 *
 *     @PersistentVariable("ambiguousInteger1")
 *     private int myInt1;
 *
 *     @PersistentVariable
 *     private String nonAmbiguous
 *
 *     @DeserializationConstructor
 *     public MyMovable(
 *         AbstractMovable.Holder base,
 *         @PersistentVariable("ambiguousInteger0") int0,
 *         @PersistentVariable("ambiguousInteger1") int1,
 *         String str)
 *     {
 *         super(base);
 *         this.myInt0 = int0;
 *         this.myInt1 = int1;
 *         this.nonAmbiguous = str;
 *     }
 *     ...
 * }}</pre>
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

    private final Int2ObjectMap<DeserializationConstructor> constructors;

    private final @Nullable DeserializationConstructor defaultConstructor;

    public MovableSerializer(Class<T> movableClass)
    {
        this.movableClass = movableClass;

        if (Modifier.isAbstract(movableClass.getModifiers()))
            throw new IllegalArgumentException("The MovableSerializer only works for concrete classes!");

        this.fields = findAnnotatedFields(movableClass);
        this.constructors = getConstructors(movableClass);
        this.defaultConstructor = constructors.get(-1);
    }

    public MovableSerializer(MovableType type)
    {
        //noinspection unchecked
        this((Class<T>) type.getMovableClass());
    }

    private static Int2ObjectMap<DeserializationConstructor> getConstructors(Class<?> movableClass)
    {
        final List<Constructor<?>> annotatedCtors = ReflectionBuilder
            .findConstructor(movableClass)
            .withAnnotations(Deserialization.class)
            .setAccessible().getAll();

        if (annotatedCtors.isEmpty())
            throw new IllegalStateException(
                "Could not find any deserialization constructors in class: " + movableClass);

        final IntSet versions = new IntArraySet(annotatedCtors.size());
        final var deserializationCtors = new Int2ObjectOpenHashMap<DeserializationConstructor>(annotatedCtors.size());

        for (final Constructor<?> annotatedCtor : annotatedCtors)
        {
            final int version = Objects.requireNonNull(annotatedCtor.getAnnotation(Deserialization.class)).version();

            if (!versions.add(version))
                throw new IllegalArgumentException(
                    "Found multiple deserialization constructors for version " + version +
                        " in class: " + movableClass);

            final DeserializationConstructor ctor = new DeserializationConstructor(
                version, annotatedCtor, getConstructorParameters(annotatedCtor));

            deserializationCtors.put(version, ctor);
        }

        return deserializationCtors;
    }

    private static List<ConstructorParameter> getConstructorParameters(Constructor<?> ctor)
    {
        final List<ConstructorParameter> ret = new ArrayList<>(ctor.getParameterCount());
        boolean foundBase = false;
        final Set<Class<?>> unnamedParameters = new HashSet<>();
        final Set<String> namedParameters = new HashSet<>();

        for (final Parameter parameter : ctor.getParameters())
        {
            if (parameter.getType() == AbstractMovable.MovableBaseHolder.class && !foundBase)
            {
                foundBase = true;
                ret.add(new ConstructorParameter("", AbstractMovable.MovableBaseHolder.class));
                continue;
            }

            final ConstructorParameter constructorParameter = ConstructorParameter.of(parameter);
            if (constructorParameter.name == null && !unnamedParameters.add(parameter.getType()))
                throw new IllegalArgumentException(
                    "Found ambiguous parameter type " + parameter + " in constructor: " + ctor);
            if (constructorParameter.name != null && !namedParameters.add(constructorParameter.name))
                throw new IllegalArgumentException(
                    "Found ambiguous parameter name " + parameter + " in constructor: " + ctor);

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
        final List<AnnotatedField> fields = ReflectionBuilder
            .findField().inClass(movableClass)
            .withAnnotations(PersistentVariable.class)
            .checkSuperClasses()
            .setAccessible()
            .get().stream()
            .map(AnnotatedField::of)
            .toList();

        final Set<Class<?>> unnamedFields = new HashSet<>();
        final Set<String> namedFields = new HashSet<>();
        for (final AnnotatedField field : fields)
        {
            if (field.annotatedName == null && !unnamedFields.add(field.field.getType()))
                throw new IllegalArgumentException(
                    "Found ambiguous field type " + field + " in class: " + movableClass.getName());
            if (field.annotatedName != null && !namedFields.add(field.annotatedName))
                throw new IllegalArgumentException(
                    "Found ambiguous field name " + field + " in class: " + movableClass.getName());
        }

        return fields;
    }

    /**
     * Serializes the type-specific data of a movable.
     *
     * @param movable
     *     The movable.
     * @return The serialized type-specific data represented as a json string.
     */
    public String serialize(AbstractMovable movable)
        throws Exception
    {
        final HashMap<String, Object> values = new HashMap<>(fields.size());
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
        try
        {
            return JSON.toJSONString(values);
        }
        catch (Exception e)
        {
            throw new Exception("Failed to serialize data to json: " + values, e);
        }
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
     * @param version
     *     The version of the type to deserialize. See {@link MovableType#getVersion()}.
     * @param json
     *     The serialized type-specific data represented as a json string.
     * @return The newly created instance.
     */
    public T deserialize(MovableRegistry registry, AbstractMovable.MovableBaseHolder movable, int version, String json)
    {
        //noinspection unchecked
        return (T) registry.computeIfAbsent(movable.get().getUid(), () -> deserialize(movable, version, json));
    }

    @VisibleForTesting
    T deserialize(AbstractMovable.MovableBaseHolder movable, int version, String json)
    {
        @Nullable Map<String, Object> dataAsMap = null;
        try
        {
            //noinspection unchecked
            dataAsMap = JSON.parseObject(json, HashMap.class);
            return instantiate(movable, version, dataAsMap);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to deserialize movable " + movable + "\nWith Data: " + dataAsMap, e);
        }
    }

    private DeserializationConstructor getDeserializationConstructor(int version)
    {
        final @Nullable DeserializationConstructor ctor = constructors.get(version);
        //noinspection ConstantValue
        if (ctor != null)
            return ctor;

        if (defaultConstructor == null)
            throw new IllegalArgumentException(
                "Failed to find constructor for version " + version + " for type: " + movableClass.getName());
        return defaultConstructor;
    }

    @VisibleForTesting
    T instantiate(AbstractMovable.MovableBaseHolder movableBase, int version, Map<String, Object> values)
        throws Exception
    {
        if (values.size() != fields.size())
            log.atWarning().log("Expected %d arguments but received %d for type %s",
                                fields.size(), values.size(), getMovableTypeName());

        final DeserializationConstructor deserializationCtor = getDeserializationConstructor(version);

        @Nullable Object @Nullable [] deserializedParameters = null;
        try
        {
            deserializedParameters = deserializeParameters(deserializationCtor, movableBase, values);
            //noinspection unchecked
            return (T) deserializationCtor.ctor.newInstance(deserializedParameters);
        }
        catch (Exception t)
        {
            throw new Exception(
                "Failed to create new instance of type: " + getMovableTypeName() + ", with parameters: " +
                    Arrays.toString(deserializedParameters), t);
        }
    }

    private Object[] deserializeParameters(
        DeserializationConstructor deserializationCtor, AbstractMovable.MovableBaseHolder base,
        Map<String, Object> values)
    {
        final Map<Class<?>, Object> classes = new HashMap<>(values.size());
        for (final var entry : values.entrySet())
            classes.put(entry.getValue().getClass(), entry.getValue());

        final Object[] ret = new Object[deserializationCtor.parameters.size()];
        int idx = -1;
        for (final ConstructorParameter param : deserializationCtor.parameters)
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
                                  idx, (param.name == null ? param.type : param.name),
                                  (param.name == null ? classes : values)), e);
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

    /**
     * @param version
     *     The version of the deserialization constructor. See {@link Deserialization#version()}.
     * @param ctor
     *     A constructor in the {@link #movableClass} that takes exactly 1 argument of the type {@link MovableBase} and
     *     is annotated with {@link Deserialization}.
     * @param parameters
     *     The parameters of the {@link #ctor}.
     */
    private record DeserializationConstructor(int version, Constructor<?> ctor, List<ConstructorParameter> parameters)
    {
    }
}
