package nl.pim16aap2.bigDoors.reflection;

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
    public @NotNull EnumValuesFinderInSource inEnum(@NotNull Class<?> source)
    {
        return new EnumValuesFinderInSource(source);
    }

    /**
     * Represents the second step of the enum values finder, after the source class has been specified.
     */
    public static final class EnumValuesFinderInSource
        extends ReflectionFinder<@NotNull Object[], EnumValuesFinderInSource>
    {
        private final @NotNull Class<?> source;

        private EnumValuesFinderInSource(@NotNull Class<?> source)
        {
            this.source = Objects.requireNonNull(source, "Source class cannot be null!");
        }

        @Override
        public @NotNull Object[] get()
        {
            return ReflectionUtils.getEnumValues(source);
        }
    }
}
