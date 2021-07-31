package nl.pim16aap2.bigDoors.reflection;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
     * @param source The class to analyze.
     * @return The next step in the enum values finding process.
     */
    @Contract("_ -> new")
    public EnumValuesFinderInSource inClass(@NotNull Class<?> source)
    {
        return new EnumValuesFinderInSource(source);
    }

    /**
     * Represents the second step of the enum values finder, after the source class has been specified.
     */
    public static class EnumValuesFinderInSource extends ReflectionFinder<Object[], EnumValuesFinderInSource>
    {
        private final @NotNull Class<?> source;

        private EnumValuesFinderInSource(@NotNull Class<?> source)
        {
            this.source = Objects.requireNonNull(source, "Source class cannot be null!");
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException When the source class is not an enum.
         */
        @Override
        public @NotNull Object[] get()
        {
            return ReflectionBackend.getEnumValues(source);
        }

        /**
         * Creates a new {@link NamedEnumValueFinder} to retrieve the enum value by its name.
         *
         * @param name The name of the enum value to look for.
         * @return The new {@link NamedEnumValueFinder}.
         */
        @Contract("_ -> new")
        public NamedEnumValueFinder withName(@NotNull String name)
        {
            return new NamedEnumValueFinder(source, Objects
                .requireNonNull(name, "Name of named enum constant cannot be null!"));
        }

        /**
         * Creates a new {@link IndexedEnumValueFinder} to retrieve the enum value by its index.
         *
         * @param index The index of the enum value to look for.
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
        private final @NotNull Class<?> source;
        private final @NotNull String name;

        private NamedEnumValueFinder(@NotNull Class<?> source, @NotNull String name)
        {
            this.source = source;
            this.name = name;
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException When the source class is not an enum.
         */
        @Override
        public Object get()
        {
            return ReflectionBackend.getNamedEnumConstant(nonnull, source, name);
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinder} to retrieve an enum value by its index.
     */
    public static final class IndexedEnumValueFinder extends ReflectionFinder<Object, NamedEnumValueFinder>
    {
        private final @NotNull Class<?> source;
        private final int index;

        private IndexedEnumValueFinder(@NotNull Class<?> source, int index)
        {
            this.source = source;
            this.index = index;
        }

        /**
         * {@inheritDoc}
         *
         * @throws IllegalStateException When the source class is not an enum.
         */
        @Override
        public Object get()
        {
            final Object[] values = ReflectionBackend.getEnumValues(source);
            if (index >= values.length)
            {
                if (nonnull)
                    // An ArrayIndexOutOfBoundsException might seem more appropriate here,
                    // but to keep it in line with the other finders, we'll throw an NPE.
                    throw new NullPointerException(
                        String.format("Requested index %d exceeded the %d enum values found in class %s!",
                                      index, values.length, source.getName()));
                return null;
            }

            return values[index];
        }
    }
}
