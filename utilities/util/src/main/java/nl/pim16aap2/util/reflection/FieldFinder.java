package nl.pim16aap2.util.reflection;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static nl.pim16aap2.util.reflection.ReflectionBackend.optionalModifiersToString;

/**
 * Represents a {@link ReflectionFinder} for fields.
 * <p>
 * This class can be used to retrieve fields by name and by type. When retrieving by type, it can also return all fields
 * of that type in the given source class.
 * <p>
 * Calls should be chained, as the instance can change between invocations of some methods.
 */
@SuppressWarnings("unused")
public class FieldFinder
{
    /**
     * Sets the class this field finder will search in for fields.
     *
     * @param source
     *     The class to analyze.
     * @return The next step in the field finding process.
     */
    @CheckReturnValue @Contract(pure = true)
    public FieldFinderInSource inClass(Class<?> source)
    {
        return new FieldFinderInSource(source);
    }

    /**
     * Represents the second step of the field finder, after the source class has been specified.
     */
    public static final class FieldFinderInSource
    {
        private final Class<?> source;

        private FieldFinderInSource(Class<?> source)
        {
            this.source = Objects.requireNonNull(source, "Source class cannot be null!");
        }

        /**
         * Creates a new {@link NamedFieldFinder} to retrieve the field by its name.
         *
         * @param name
         *     The name of the field to look for.
         * @return The new {@link NamedFieldFinder}.
         */
        @CheckReturnValue @Contract(pure = true)
        public NamedFieldFinder withName(String name)
        {
            return new NamedFieldFinder(name, source);
        }

        /**
         * Creates a new {@link TypedFieldFinder} to retrieve the field by its type.
         *
         * @param type
         *     The type of the field to look for.
         * @return The new {@link TypedFieldFinder}.
         */
        @CheckReturnValue @Contract(pure = true)
        public <T> TypedFieldFinder<T> ofType(Class<T> type)
        {
            return new TypedFieldFinder<>(source, type);
        }

        /**
         * Creates a new {@link MultipleFieldsFinder} to retrieve all fields of a given type.
         *
         * @param type
         *     The type of the fields to look for.
         * @return The new {@link MultipleFieldsFinder}.
         */
        @CheckReturnValue @Contract(pure = true)
        public MultipleFieldsFinder allOfType(Class<?> type)
        {
            return new MultipleFieldsFinder(source, type);
        }

        /**
         * Creates a new {@link MultipleFieldsFinder} to retrieve all fields annotated with the provided annotations.
         *
         * @param annotations
         *     The type of the fields to look for.
         * @return The new {@link MultipleFieldsFinder}.
         */
        @SafeVarargs @CheckReturnValue @Contract(pure = true)
        public final MultipleFieldsFinder withAnnotations(Class<? extends Annotation>... annotations)
        {
            return new MultipleFieldsFinder(source, annotations);
        }
    }

    private static abstract class ReflectionFieldFinder<T, U extends ReflectionFinder<T, U>, V>
        extends ReflectionFinder<T, U>
        implements IAccessibleSetter<U>, IAnnotationFinder<U>
    {
        protected boolean setAccessible = false;
        protected boolean checkSuperClasses = false;
        @SuppressWarnings("unchecked")
        protected Class<? extends Annotation>[] annotations = new Class[0];

        @Override
        public U setAccessible()
        {
            setAccessible = true;
            //noinspection unchecked
            return (U) this;
        }

        @Override
        @SafeVarargs
        public final U withAnnotations(Class<? extends Annotation>... annotations)
        {
            this.annotations = annotations;
            //noinspection unchecked
            return (U) this;
        }

        /**
         * Used to configure the field finder to include the source class's superclasses when searching for the target
         * field.
         *
         * @return The current field finder instance.
         */
        public U checkSuperClasses()
        {
            this.checkSuperClasses = true;
            //noinspection unchecked
            return (U) this;
        }
    }

    /**
     * A specialized version of {@link ReflectionFieldFinder} that only retrieves a single field.
     *
     * @param <T>
     *     The type of the source class.
     * @param <U>
     *     The type of the field.
     */
    private static abstract class SingleFieldFinder<T extends ReflectionFinder<Field, T>, U>
        extends ReflectionFieldFinder<Field, T, U>
    {
        protected abstract Class<U> getType();

        // We throw our own ClassCastException, ignoring the original stack trace.
        @SuppressWarnings("PMD.PreserveStackTrace")
        private @Nullable U getNullable(Field field, @Nullable Object instance)
        {
            final Class<U> type = getType();

            @Nullable Object object = null;
            try
            {
                object = field.get(instance);
                return type.cast(object);
            }
            catch (IllegalAccessException exception)
            {
                throw new IllegalArgumentException(
                    String.format(
                        "Failed to access field %s in instance %s. Was set accessible: %s",
                        field.toGenericString(),
                        instance,
                        setAccessible),
                    exception
                );
            }
            catch (ClassCastException e)
            {
                throw new ClassCastException(String.format(
                    "Failed to cast object of field %s from type %s to type %s in instance %s",
                    field,
                    object == null ? "null" : object.getClass().getName(),
                    type.getName(),
                    instance)
                );
            }
        }

        /**
         * Retrieves the object of the field from the given instance.
         * <p>
         * The field will be retrieved using {@link #get()} first.
         *
         * @param instance
         *     The instance to retrieve the field from.
         *     <p>
         *     This can be null if the field is static.
         *     <p>
         * @return The object of the field from the given instance.
         *
         * @throws NullPointerException
         *     If the field could not be found, if the instance was null and the field was not static, or if the object
         *     of the field was null.
         * @throws ClassCastException
         *     If the field could be found, but could not be cast to the correct type.
         * @throws IllegalArgumentException
         *     If the field could be found, but could not be accessed or
         */
        public @Nullable U get(@Nullable Object instance)
        {
            final Field field = get();
            return Objects.requireNonNull(
                getNullable(field, instance),
                String.format(
                    "Failed to retrieve object of field %s in instance %s",
                    field.toGenericString(),
                    instance)
            );
        }

        /**
         * Retrieves the object of the field from the given instance.
         * <p>
         * The field will be retrieved using {@link #get()} first.
         *
         * @param instance
         *     The instance to retrieve the field from.
         *     <p>
         *     This can be null if the field is static.
         *     <p>
         * @return The object of the field from the given instance.
         *
         * @throws NullPointerException
         *     If the field could not be found or if the instance was null and the field was not static.
         * @throws ClassCastException
         *     If the field could be found, but could not be cast to the correct type.
         * @throws IllegalArgumentException
         *     If the field could be found, but could not be accessed or
         */
        public @Nullable U getNullable(@Nullable Object instance)
        {
            final Field field = get();
            return getNullable(field, instance);
        }

        /**
         * Sets the value of the field in the given instance.
         * <p>
         * The field will be retrieved using {@link #get()} first.
         *
         * @param instance
         *     The instance to set the field in.
         * @param value
         *     The value to set the field to.
         * @throws NullPointerException
         *     If the field could not be found or if the instance was null and the field was not static.
         * @throws IllegalArgumentException
         *     If the field could not be accessed, or if the field could not be set to the given value.
         */
        public void set(@Nullable Object instance, @Nullable U value)
        {
            final Field field = get();
            try
            {
                field.set(instance, value);
            }
            catch (IllegalAccessException exception)
            {
                throw new IllegalArgumentException(
                    String.format(
                        "Failed to set field %s in instance %s to value %s. Was set accessible: %s",
                        field.toGenericString(),
                        instance,
                        value,
                        setAccessible),
                    exception
                );
            }
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinder} to retrieve a field by its name.
     */
    public static final class NamedFieldFinder
        extends SingleFieldFinder<NamedFieldFinder, Object>
    {
        private final String name;
        private final Class<?> source;
        private @Nullable Class<?> fieldType = null;

        private NamedFieldFinder(String name, Class<?> source)
        {
            this.name = Objects.requireNonNull(name, "Name cannot be null!");
            this.source = source;
        }

        @Override
        public Field get()
        {
            //noinspection ConstantConditions
            return Objects.requireNonNull(
                getNullable(),
                String.format(
                    "Failed to find field %s[%s %s %s.%s]. Super classes were %s.",
                    ReflectionBackend.formatAnnotations(annotations),
                    optionalModifiersToString(modifiers),
                    ReflectionBackend.formatOptionalValue(fieldType, Class::getName),
                    source.getName(), name,
                    checkSuperClasses ? "included" : "excluded"
                ));
        }

        @Override
        public @Nullable Field getNullable()
        {
            return ReflectionBackend.getField(
                source,
                name,
                modifiers,
                fieldType,
                setAccessible,
                checkSuperClasses,
                annotations
            );
        }

        /**
         * Specifies the type the field should have.
         * <p>
         * If this is set to null (default), the type of the field will be ignored.
         *
         * @param fieldType
         *     The type the field should have.
         * @return The current finder instance.
         */
        public NamedFieldFinder ofType(@Nullable Class<?> fieldType)
        {
            this.fieldType = fieldType;
            return this;
        }

        @Override
        protected Class<Object> getType()
        {
            return Object.class;
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinder} to retrieve a field by its type.
     */
    public static final class TypedFieldFinder<T>
        extends SingleFieldFinder<TypedFieldFinder<T>, T>
    {
        private final Class<?> source;
        private final Class<T> fieldType;

        private TypedFieldFinder(Class<?> source, Class<T> fieldType)
        {
            this.source = source;
            this.fieldType = Objects.requireNonNull(fieldType, "Field type cannot be null!");
        }

        @Override
        public Field get()
        {
            //noinspection ConstantConditions
            return Objects.requireNonNull(
                getNullable(),
                String.format(
                    "Failed to find field: %s[%s %s %s.[*]]. Super classes were %s.",
                    ReflectionBackend.formatAnnotations(annotations),
                    optionalModifiersToString(modifiers),
                    fieldType.getName(),
                    source.getName(),
                    checkSuperClasses ? "included" : "excluded")
            );
        }

        @Override
        public @Nullable Field getNullable()
        {
            return ReflectionBackend.getField(
                source,
                modifiers,
                fieldType,
                setAccessible,
                checkSuperClasses,
                annotations
            );
        }

        @Override
        protected Class<T> getType()
        {
            return fieldType;
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinder} to retrieve all fields in a class of a given type.
     */
    public static final class MultipleFieldsFinder
        extends ReflectionFieldFinder<List<Field>, MultipleFieldsFinder, Object[]>
    {
        private final Class<?> source;
        private @Nullable Class<?> fieldType;
        private int expected = -1;
        private int atMost = -1;
        private int atLeast = -1;

        private MultipleFieldsFinder(Class<?> source, Class<?> fieldType)
        {
            this.source = source;
            this.fieldType = fieldType;
        }

        @SafeVarargs
        private MultipleFieldsFinder(Class<?> source, Class<? extends Annotation>... annotations)
        {
            this.source = source;
            this.annotations = annotations;
        }

        /**
         * {@inheritDoc}
         *
         * @return The fields that were requested, if any were found.
         *
         * @throws NullPointerException
         *     If no fields could be found.
         */
        @Override
        public List<Field> get()
        {
            return Objects.requireNonNull(getResult(true));
        }

        /**
         * {@inheritDoc}
         *
         * @return The fields that were requested, if any were found, otherwise an empty list.
         */
        @Override
        public List<Field> getNullable()
        {
            return getResult(false);
        }

        @CheckReturnValue @Contract(pure = true)
        private List<Field> getResult(boolean nonnull)
        {
            final List<Field> found =
                ReflectionBackend.getFields(
                    source,
                    modifiers,
                    fieldType,
                    setAccessible,
                    checkSuperClasses,
                    annotations
                );

            if (expected >= 0 && expected != found.size())
                return handleInvalid(
                    nonnull,
                    "Expected %d fields of type %s in class %s " +
                        "with modifiers %d annotated with %s, but found %d.  Super classes were %s.",
                    expected,
                    fieldType,
                    source,
                    modifiers,
                    annotations,
                    found.size(),
                    checkSuperClasses ? "included" : "excluded"
                );

            if (atMost >= 0 && found.size() > atMost)
                return handleInvalid(
                    nonnull,
                    "Expected at most %d fields of type %s in class %s " +
                        "with modifiers %d annotated with %s, but found %d.  Super classes were %s.",
                    atMost,
                    fieldType,
                    source,
                    modifiers,
                    annotations,
                    found.size(),
                    checkSuperClasses ? "included" : "excluded"
                );

            if (atLeast >= 0 && found.size() < atLeast)
                return handleInvalid(
                    nonnull,
                    "Expected at least %d fields of type %s in class %s " +
                        "with modifiers %d annotated with %s, but found %d.  Super classes were %s.",
                    atLeast,
                    fieldType,
                    source,
                    modifiers,
                    annotations,
                    found.size(),
                    checkSuperClasses ? "included" : "excluded"
                );
            return found;
        }

        // Suppress AvoidThrowingNullPointerException because we want to throw an NPE manually
        // when nothing is found to keep in line with the rest of the API.
        @SuppressWarnings("PMD.AvoidThrowingNullPointerException")
        @Contract(value = "true, _, _, _, _, _, _, _, _ -> fail", pure = true)
        private static List<Field> handleInvalid(
            boolean nonnull,
            String str,
            int val,
            @Nullable Class<?> fieldType,
            Class<?> source,
            int modifiers,
            Class<? extends Annotation>[] annotations,
            int foundSize,
            String checkSuperClasses)
        {
            if (nonnull)
                throw new NullPointerException(
                    String.format(
                        str,
                        val,
                        fieldType,
                        modifiers,
                        Arrays.toString(annotations),
                        fieldType,
                        checkSuperClasses)
                );
            return Collections.emptyList();
        }

        /**
         * Configures the lower bound number of fields that have to be found.
         * <p>
         * For example, when this is set to 2 and only 1 field could be found with the current configuration,
         * {@link #getNullable()} will either return null (when null values are allowed) or throw a
         * {@link IllegalStateException} (default).
         *
         * @param val
         *     The minimum number of fields that have to be found for this finder to be able to complete successfully.
         * @return The instance of the current finder.
         */
        public MultipleFieldsFinder atLeast(int val)
        {
            atLeast = val;
            return this;
        }

        /**
         * Configures the upper bound number of fields that have to be found.
         * <p>
         * For example, when this is set to 2 and 3 fields could be found with the current configuration,
         * {@link #getNullable()} will either return null (when null values are allowed) or throw a
         * {@link IllegalStateException} (default).
         *
         * @param val
         *     The maximum number of fields that can be found for this finder to be able to complete successfully.
         * @return The instance of the current finder.
         */
        public MultipleFieldsFinder atMost(int val)
        {
            atMost = val;
            return this;
        }

        /**
         * Configures the exact number of fields that have to be found.
         * <p>
         * For example, when this is set to 2 and 1 or 3 fields could be found with the current configuration,
         * {@link #getNullable()} will either return null (when null values are allowed) or throw a
         * {@link IllegalStateException} (default).
         *
         * @param val
         *     The exact number of fields that must be found for this finder to be able to complete successfully.
         * @return The instance of the current finder.
         */
        public MultipleFieldsFinder exactCount(int val)
        {
            expected = val;
            return this;
        }
    }
}
