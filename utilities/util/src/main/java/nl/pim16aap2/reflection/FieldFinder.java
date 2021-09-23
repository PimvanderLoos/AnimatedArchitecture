package nl.pim16aap2.reflection;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static nl.pim16aap2.reflection.ReflectionBackend.optionalModifiersToString;

/**
 * Represents a {@link ReflectionFinder} for fields.
 * <p>
 * This class can be used to retrieve fields by name and by type. When retrieving by type, it can also return all fields
 * of that type in the given source class.
 * <p>
 * Calls should be chained, as the instance can change between invocations of some methods.
 *
 * @author Pim
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
    @Contract("_ -> new")
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
        @Contract("_ -> new")
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
        @Contract("_ -> new")
        public TypedFieldFinder ofType(Class<?> type)
        {
            return new TypedFieldFinder(source, type);
        }

        /**
         * Creates a new {@link TypedMultipleFieldsFinder} to retrieve all the field with a given type.
         *
         * @param type
         *     The type of the fields to look for.
         * @return The new {@link TypedMultipleFieldsFinder}.
         */
        @Contract("_ -> new")
        public TypedMultipleFieldsFinder allOfType(Class<?> type)
        {
            return new TypedMultipleFieldsFinder(source, type);
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinder} to retrieve a field by its name.
     */
    public static final class NamedFieldFinder
        extends ReflectionFinder<Field, NamedFieldFinder>
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
        public Field getRequired()
        {
            //noinspection ConstantConditions
            return Objects.requireNonNull(ReflectionBackend.getField(source, name, modifiers, fieldType),
                                          String.format("Failed to find field [%s %s %s.%s].",
                                                        optionalModifiersToString(modifiers),
                                                        ReflectionBackend.formatOptionalValue(fieldType,
                                                                                              Class::getName),
                                                        source.getName(), name));
        }

        @Override
        public @Nullable Field get()
        {
            return ReflectionBackend.getField(source, name, modifiers, fieldType);
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
        @Contract("_ -> this")
        public NamedFieldFinder ofType(@Nullable Class<?> fieldType)
        {
            this.fieldType = fieldType;
            return this;
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinder} to retrieve a field by its type.
     */
    public static final class TypedFieldFinder
        extends ReflectionFinder<Field, TypedFieldFinder>
    {
        private final Class<?> source;
        private final Class<?> fieldType;

        private TypedFieldFinder(Class<?> source, Class<?> fieldType)
        {
            this.source = source;
            this.fieldType = Objects.requireNonNull(fieldType, "Field type cannot be null!");
        }

        @Override
        public Field getRequired()
        {
            //noinspection ConstantConditions
            return Objects.requireNonNull(get(), String.format("Failed to find field: [%s %s %s.[*]].",
                                                               optionalModifiersToString(modifiers),
                                                               fieldType.getName(), source.getName()));
        }

        @Override
        public @Nullable Field get()
        {
            return ReflectionBackend.getField(source, modifiers, fieldType);
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinder} to retrieve all fields in a class of a given type.
     */
    public static final class TypedMultipleFieldsFinder
        extends ReflectionFinder<List<Field>, TypedMultipleFieldsFinder>
    {
        private final Class<?> source;
        private final Class<?> fieldType;
        private int expected = -1;
        private int atMost = -1;
        private int atLeast = -1;

        private TypedMultipleFieldsFinder(Class<?> source, Class<?> fieldType)
        {
            this.source = source;
            this.fieldType = Objects.requireNonNull(fieldType, "Field type cannot be null!");
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
        public List<Field> getRequired()
        {
            return Objects.requireNonNull(getResult(true));
        }

        /**
         * {@inheritDoc}
         *
         * @return The fields that were requested, if any were found, otherwise an empty list.
         */
        @Override
        public List<Field> get()
        {
            return getResult(false);
        }

        private List<Field> getResult(boolean nonnull)
        {
            final List<Field> found = ReflectionBackend.getFields(source, modifiers, fieldType);
            if (expected >= 0 && expected != found.size())
                return handleInvalid(nonnull, "Expected %d fields of type %s in class %s " +
                                         "with modifiers %d, but found %d",
                                     expected, fieldType, source, modifiers, found.size());

            if (atMost >= 0 && found.size() > atMost)
                return handleInvalid(nonnull, "Expected at most %d fields of type %s in class %s " +
                                         "with modifiers %d, but found %d",
                                     atMost, fieldType, source, modifiers, found.size());

            if (atLeast >= 0 && found.size() < atLeast)
                return handleInvalid(nonnull, "Expected at least %d fields of type %s in class %s " +
                                         "with modifiers %d, but found %d",
                                     atLeast, fieldType, source, modifiers, found.size());
            return found;
        }

        // Suppress AvoidThrowingNullPointerException because we want to throw an NPE manually
        // when nothing is found to keep in line with the rest of the API.
        @SuppressWarnings("PMD.AvoidThrowingNullPointerException")
        @Contract("true, _, _, _, _, _, _ -> fail")
        private static List<Field> handleInvalid(boolean nonnull, String str, int val, Class<?> fieldType,
                                                 Class<?> source, int modifiers, int foundSize)
        {
            if (nonnull)
                throw new NullPointerException(String.format(str, val, fieldType, modifiers, fieldType));
            return Collections.emptyList();
        }

        /**
         * Configures the lower bound number of fields that have to be found.
         * <p>
         * For example, when this is set to 2 and only 1 field could be found with the current configuration, {@link
         * #get()} will either return null (when null values are allowed) or throw a {@link IllegalStateException}
         * (default).
         *
         * @param val
         *     The minimum number of fields that have to be found for this finder to be able to complete successfully.
         * @return The instance of the current finder.
         */
        @Contract("_ -> this")
        public TypedMultipleFieldsFinder atLeast(int val)
        {
            atLeast = val;
            return this;
        }

        /**
         * Configures the upper bound number of fields that have to be found.
         * <p>
         * For example, when this is set to 2 and 3 fields could be found with the current configuration, {@link #get()}
         * will either return null (when null values are allowed) or throw a {@link IllegalStateException} (default).
         *
         * @param val
         *     The maximum number of fields that can be found for this finder to be able to complete successfully.
         * @return The instance of the current finder.
         */
        @Contract("_ -> this")
        public TypedMultipleFieldsFinder atMost(int val)
        {
            atMost = val;
            return this;
        }

        /**
         * Configures the exact number of fields that have to be found.
         * <p>
         * For example, when this is set to 2 and 1 or 3 fields could be found with the current configuration, {@link
         * #get()} will either return null (when null values are allowed) or throw a {@link IllegalStateException}
         * (default).
         *
         * @param val
         *     The exact number of fields that must be found for this finder to be able to complete successfully.
         * @return The instance of the current finder.
         */
        @Contract("_ -> this")
        public TypedMultipleFieldsFinder exactCount(int val)
        {
            expected = val;
            return this;
        }
    }
}
