package nl.pim16aap2.bigdoors.core.structures;

import com.alibaba.fastjson2.JSON;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.annotations.Deserialization;
import nl.pim16aap2.bigdoors.core.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.core.util.MathUtil;
import nl.pim16aap2.util.SafeStringBuilder;
import nl.pim16aap2.util.reflection.ReflectionBuilder;
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
 * Manages the serialization aspects of the structures.
 * <p>
 * The {@link PersistentVariable} annotation is used on fields to determine which fields are serialized. The
 * {@link PersistentVariable#value()} provided to the animation is used for two things:
 * <ul>
 *     <li>The name is used to provide the name under which to store the value. This means that changing this value will
 *     cause the serializer to both serialize and deserialize the associated object under a different name. Each name
 *     must be unique.</li>
 *     <li>When a constructor contains more than one parameter of a given type, the provided name is used to figure
 *     out which deserialized object to map to which parameter.</li>
 * </ul>
 * <p>
 * The {@link AbstractStructure.BaseHolder} object is always provided and does not need to be handled in any specific
 * way.
 * <p>
 * When a value is missing during deserialization, null will be substituted in its place if it is not a primitive. If
 * the type is a primitive, an exception will be thrown.
 * <p>
 * The {@link Deserialization} annotation is used for deserialization to specify which constructor to use. The
 * annotation accepts an optional integer to specify a version. This refers to {@link StructureType#getVersion()}. When
 * a structure is deserialized, this class will first attempt to use the constructor that specified the exact version
 * that matches the data that has to be deserialized. If no such constructor exists, this class will fall back to the
 * constructor that does not specify a version at all or explicitly sets the value to -1. If this constructor also does
 * not exist, an exception will be thrown.
 * <p>
 * For example:
 * <pre> {@code
 * public class MyStructure extends AbstractStructure
 * {
 *     private static final int DEFAULT_MY_INT_0 = 12;
 *     private static final int DEFAULT_MY_INT_1 = 42;
 *
 *     @PersistentVariable("ambiguousInteger0")
 *     private int myInt0;
 *
 *     @PersistentVariable("ambiguousInteger1")
 *     private int myInt1;
 *
 *     @PersistentVariable("nonAmbiguous")
 *     private String nonAmbiguous;
 *
 *     @Deserialization(version = 2)
 *     public MyStructure(
 *         AbstractStructure.Holder base,
 *         @PersistentVariable("ambiguousInteger0") int0,
 *         @PersistentVariable("ambiguousInteger1") int1,
 *         String str) // String is not ambiguous, so no name required.
 *     {
 *         super(base);
 *         this.myInt0 = int0;
 *         this.myInt1 = int1;
 *         this.nonAmbiguous = str;
 *     }
 *
 *     @Deserialization(version = 1)
 *     public MyStructure(
 *         AbstractStructure.Holder base,
 *         String str) // String is not ambiguous, so no name required.
 *     {
 *         this(base, DEFAULT_MY_INT_0, DEFAULT_MY_INT_1, str);
 *     }
 *
 *     @Deserialization // Fallback
 *     public MyStructure(AbstractStructure.Holder base)
 *     {
 *         this(base, DEFAULT_MY_INT_0, DEFAULT_MY_INT_1, "DEFAULT_NAME");
 *     }
 *     ...
 * }}</pre>
 * <p>
 * The {@link Deserialization} annotation accepts an integer version variable. This allows specifying a different
 * deserialization constructors for different versions of the structure type. When no constructor exists that explicitly
 * supports the version of a structure that has to be deserialized, the deserializer will use the constructor whose
 * annotation does not specify a specific version if it exists.
 *
 * @param <T>
 *     The type of structure.
 * @author Pim
 */
@Flogger
public final class StructureSerializer<T extends AbstractStructure>
{
    /**
     * The target class.
     */
    private final Class<T> structureClass;

    /**
     * The current version of the {@link StructureType} this serializer exists for.
     */
    private final int currentTypeVersion;

    /**
     * The list of serializable fields in the target class {@link #structureClass} that are annotated with
     * {@link PersistentVariable}.
     */
    private final List<AnnotatedField> fields;

    private final Int2ObjectMap<DeserializationConstructor> constructors;

    private final @Nullable DeserializationConstructor defaultConstructor;

    @VisibleForTesting
    public StructureSerializer(Class<T> structureClass, int currentTypeVersion)
    {
        this.structureClass = structureClass;
        this.currentTypeVersion = currentTypeVersion;

        if (Modifier.isAbstract(structureClass.getModifiers()))
            throw new IllegalArgumentException("The StructureSerializer only works for concrete classes!");

        this.fields = findAnnotatedFields(structureClass);
        this.constructors = getConstructors(structureClass);
        this.defaultConstructor = constructors.get(-1);
    }

    public StructureSerializer(StructureType type)
    {
        //noinspection unchecked
        this((Class<T>) type.getStructureClass(), type.getVersion());
    }

    private static Int2ObjectMap<DeserializationConstructor> getConstructors(Class<?> structureClass)
    {
        final List<Constructor<?>> annotatedCtors = ReflectionBuilder
            .findConstructor(structureClass)
            .withAnnotations(Deserialization.class)
            .setAccessible().getAll();

        if (annotatedCtors.isEmpty())
            throw new IllegalStateException(
                "Could not find any deserialization constructors in class: " + structureClass);

        final IntSet versions = new IntArraySet(annotatedCtors.size());
        final var deserializationCtors = new Int2ObjectOpenHashMap<DeserializationConstructor>(annotatedCtors.size());

        for (final Constructor<?> annotatedCtor : annotatedCtors)
        {
            final int version = Objects.requireNonNull(annotatedCtor.getAnnotation(Deserialization.class)).version();

            if (!versions.add(version))
                throw new IllegalArgumentException(
                    "Found multiple deserialization constructors for version " + version +
                        " in class: " + structureClass);

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
            if (parameter.getType() == AbstractStructure.BaseHolder.class && !foundBase)
            {
                foundBase = true;
                ret.add(new ConstructorParameter("", AbstractStructure.BaseHolder.class));
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
                "Could not found parameter StructureBaseHolder in deserialization constructor: " + ctor);

        return ret;
    }

    private static List<AnnotatedField> findAnnotatedFields(Class<? extends AbstractStructure> structureClass)
        throws UnsupportedOperationException
    {
        final List<AnnotatedField> fields = ReflectionBuilder
            .findField().inClass(structureClass)
            .withAnnotations(PersistentVariable.class)
            .checkSuperClasses()
            .setAccessible()
            .get().stream()
            .map(AnnotatedField::of)
            .toList();

        final Set<String> namedFields = new HashSet<>(MathUtil.ceil(1.25 * fields.size()));
        for (final AnnotatedField field : fields)
            if (!namedFields.add(field.name))
                throw new IllegalArgumentException(
                    "Found ambiguous field name " + field + " in class: " + structureClass.getName());

        return fields;
    }

    /**
     * Serializes the type-specific data of a structure.
     *
     * @param structure
     *     The structure.
     * @return The serialized type-specific data represented as a json string.
     */
    public String serialize(AbstractStructure structure)
        throws Exception
    {
        final HashMap<String, Object> values = new HashMap<>(MathUtil.ceil(1.25 * fields.size()));
        for (final AnnotatedField field : fields)
            try
            {
                values.put(field.name, field.field.get(structure));
            }
            catch (IllegalAccessException e)
            {
                throw new Exception(String.format("Failed to get value of field %s (type %s) for structure type %s!",
                                                  field.name(), field.typeName(), getStructureTypeName()), e);
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
     * Deserializes the serialized type-specific data of a structure.
     * <p>
     * The structure and the deserialized data are then used to create an instance of the structure type.
     *
     * @param registry
     *     The registry to use for any potential registration.
     * @param structure
     *     The base structure data.
     * @param version
     *     The version of the type to deserialize. See {@link StructureType#getVersion()}.
     * @param json
     *     The serialized type-specific data represented as a json string.
     * @return The newly created instance.
     */
    public T deserialize(StructureRegistry registry, AbstractStructure.BaseHolder structure, int version, String json)
    {
        //noinspection unchecked
        return (T) registry.computeIfAbsent(structure.get().getUid(), () -> deserialize(structure, version, json));
    }

    @VisibleForTesting
    T deserialize(AbstractStructure.BaseHolder structure, int version, String json)
    {
        @Nullable Map<String, Object> dataAsMap = null;
        try
        {
            //noinspection unchecked
            dataAsMap = JSON.parseObject(json, HashMap.class);
            return instantiate(structure, version, dataAsMap);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to deserialize structure " + structure + "\nWith Data: " + dataAsMap, e);
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
                "Failed to find constructor for version " + version + " for type: " + structureClass.getName());
        return defaultConstructor;
    }

    @VisibleForTesting
    T instantiate(AbstractStructure.BaseHolder structureBase, int version, Map<String, Object> values)
        throws Exception
    {
        if (values.size() != fields.size())
            log.atWarning().log("Expected %d arguments but received %d for type %s",
                                fields.size(), values.size(), getStructureTypeName());

        if (version > currentTypeVersion)
            throw new IllegalArgumentException(
                String.format("Failed to deserialize structure %d! Data version %d is higher than type version %d!",
                              structureBase.get().getUid(), version, this.currentTypeVersion));

        final DeserializationConstructor deserializationCtor = getDeserializationConstructor(version);

        @Nullable Object @Nullable [] deserializedParameters = null;
        try
        {
            deserializedParameters = deserializeParameters(deserializationCtor, structureBase, values);
            //noinspection unchecked
            return (T) deserializationCtor.ctor.newInstance(deserializedParameters);
        }
        catch (Exception t)
        {
            throw new Exception(
                "Failed to create new instance of type: " + getStructureTypeName() + ", with parameters: " +
                    Arrays.toString(deserializedParameters), t);
        }
    }

    private Object[] deserializeParameters(
        DeserializationConstructor deserializationCtor, AbstractStructure.BaseHolder base,
        Map<String, Object> values)
    {
        final Map<Class<?>, Object> classes = new HashMap<>(MathUtil.ceil(1.25 * values.size()));
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
                if (param.type == AbstractStructure.BaseHolder.class)
                    data = base;
                else if (param.name != null)
                    data = getDeserializedObject(base, param.type, values, param.name);
                else
                    data = getDeserializedObject(base, param.type, classes, param.type);

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
        AbstractStructure.BaseHolder base, Class<?> target, Map<T, Object> map, T key)
    {
        final @Nullable Object ret = map.get(key);
        if (ret != null)
            return postProcessDeserializedObject(target, ret);

        if (!map.containsKey(key))
            log.atSevere().log("No value found for key '%s' for structure: %s", key, base);

        return null;
    }

    private static Object postProcessDeserializedObject(Class<?> target, Object obj)
    {
        if (target.isAssignableFrom(obj.getClass()))
            return obj;
        if (target.isEnum() && obj instanceof String enumFieldName)
            return ReflectionBuilder.findEnumValues(target).withName(enumFieldName).get();

        throw new IllegalStateException("Object '" + obj + "' is not an instance of '" + target.getName() + "'");
    }

    public String getStructureTypeName()
    {
        return structureClass.getName();
    }

    @Override
    public String toString()
    {
        final SafeStringBuilder sb = new SafeStringBuilder("StructureSerializer: ")
            .append(getStructureTypeName())
            .append(", fields:\n");

        for (final AnnotatedField field : fields)
            sb.append("* Type: ").append(field.typeName())
              .append(", name: \"").append(field.name)
              .append("\"\n");
        return sb.toString();
    }

    private record AnnotatedField(Field field, String name)
    {
        public static AnnotatedField of(Field field)
        {
            verifyFieldType(field);
            final String name = field.getAnnotation(PersistentVariable.class).value();
            if (name.isBlank())
                throw new IllegalArgumentException("Received black name for PersistentVariable field: " + field);
            return new AnnotatedField(field, name);
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
     *     A constructor in the {@link #structureClass} that takes exactly 1 argument of the type {@link StructureBase}
     *     and is annotated with {@link Deserialization}.
     * @param parameters
     *     The parameters of the {@link #ctor}.
     */
    private record DeserializationConstructor(int version, Constructor<?> ctor, List<ConstructorParameter> parameters)
    {
    }
}
