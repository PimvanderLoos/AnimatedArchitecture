package nl.pim16aap2.reflection;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a {@link ReflectionFinder} for enum values.
 * <p>
 * This class can be used to retrieve enum values.
 * <p>
 * Calls should be chained, as the instance can change between invocations of some methods.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public class EnumValuesFinder
{
    /**
     * Sets the (enum)class this enum values finder will search in for enum values.
     *
     * @param source
     *     The class to analyze.
     * @return The next step in the enum values finding process.
     */
    @Contract("_ -> new")
    public EnumValuesFinderInSource inClass(Class<?> source)
    {
        return new EnumValuesFinderInSource(source);
    }

    /**
     * Represents the second step of the enum values finder, after the source class has been specified.
     */
    public static class EnumValuesFinderInSource extends ReflectionFinder<Object[], EnumValuesFinderInSource>
    {
        private final Class<?> source;

        private EnumValuesFinderInSource(Class<?> source)
        {
            this.source = Objects.requireNonNull(source, "Source class cannot be null!");
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException
         *     When the source class is not an enum.
         */
        @Override
        public Object[] getRequired()
        {
            return Objects.requireNonNull(get());
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException
         *     When the source class is not an enum.
         */
        @Override
        public Object[] get()
        {
            return ReflectionBackend.getEnumValues(source);
        }

        /**
         * Creates a new {@link NamedEnumValueFinder} to retrieve the enum value by its name.
         *
         * @param name
         *     The name of the enum value to look for.
         * @return The new {@link NamedEnumValueFinder}.
         */
        @Contract("_ -> new")
        public NamedEnumValueFinder withName(String name)
        {
            return new NamedEnumValueFinder(source, Objects
                .requireNonNull(name, "Name of named enum constant cannot be null!"));
        }

        /**
         * Creates a new {@link IndexedEnumValueFinder} to retrieve the enum value by its index.
         *
         * @param index
         *     The index of the enum value to look for.
         * @return The new {@link IndexedEnumValueFinder}.
         */
        @Contract("_ -> new")
        public IndexedEnumValueFinder atIndex(int index)
        {
            return new IndexedEnumValueFinder(source, index);
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinder} to retrieve an enum value by its name.
     */
    public static final class NamedEnumValueFinder extends ReflectionFinder<Object, NamedEnumValueFinder>
    {
        private final Class<?> source;
        private final String name;

        private NamedEnumValueFinder(Class<?> source, String name)
        {
            this.source = source;
            this.name = name;
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException
         *     When the source class is not an enum.
         */
        @Override
        public Object getRequired()
        {
            //noinspection ConstantConditions
            return Objects.requireNonNull(ReflectionBackend.getNamedEnumConstant(source, name),
                                          String.format("Failed to find enum value: [%s.%s].",
                                                        source.getName(), name));
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException
         *     When the source class is not an enum.
         */
        @Override
        public @Nullable Object get()
        {
            return ReflectionBackend.getNamedEnumConstant(source, name);
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinder} to retrieve an enum value by its index.
     */
    public static final class IndexedEnumValueFinder extends ReflectionFinder<Object, NamedEnumValueFinder>
    {
        private final Class<?> source;
        private final int index;

        private IndexedEnumValueFinder(Class<?> source, int index)
        {
            this.source = source;
            this.index = index;
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException
         *     When the source class is not an enum.
         */
        // Suppress AvoidThrowingNullPointerException because we want to throw an NPE manually
        // when nothing is found to keep in line with the rest of the API.
        @SuppressWarnings("PMD.AvoidThrowingNullPointerException")
        @Override
        public Object getRequired()
        {
            final Object[] values = ReflectionBackend.getEnumValues(source);
            if (index >= values.length)
                // An ArrayIndexOutOfBoundsException might seem more appropriate here,
                // but to keep it in line with the other finders, we'll throw an NPE.
                throw new NullPointerException(
                    String.format("Requested index %d exceeded the %d enum values found in class %s!",
                                  index, values.length, source.getName()));
            return values[index];
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException
         *     When the source class is not an enum.
         */
        @Override
        public @Nullable Object get()
        {
            final Object[] values = ReflectionBackend.getEnumValues(source);
            return index < values.length ? values[index] : null;
        }
    }
}
