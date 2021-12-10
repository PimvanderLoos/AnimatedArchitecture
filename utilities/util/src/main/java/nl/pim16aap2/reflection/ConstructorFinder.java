package nl.pim16aap2.reflection;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.Objects;

/**
 * Represents a {@link ReflectionFinder} that can find {@link Constructor}s.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public class ConstructorFinder
{
    /**
     * Sets the class this constructor finder will search in for constructors.
     *
     * @param source
     *     The class to analyze.
     * @return The next step in the constructor finding process.
     */
    @CheckReturnValue @Contract(pure = true)
    public ConstructorFinderInSource inClass(Class<?> source)
    {
        return new ConstructorFinderInSource(Objects.requireNonNull(source, "Source class cannot be null!"));
    }

    /**
     * Represents an implementation of {@link ReflectionFinder}
     */
    public static final class ConstructorFinderInSource
        extends ReflectionFinder.ReflectionFinderWithParameters<Constructor<?>, ConstructorFinderInSource>
    {
        private final Class<?> source;

        private ConstructorFinderInSource(Class<?> source)
        {
            this.source = source;
        }

        @Override
        public Constructor<?> get()
        {
            return Objects.requireNonNull(getNullable(), String.format(
                "Failed to find constructor [%s %s(%s)].",
                ReflectionBackend.optionalModifiersToString(modifiers),
                source.getName(),
                ReflectionBackend.formatOptionalValue(parameters)));
        }

        @Override
        public @Nullable Constructor<?> getNullable()
        {
            return ReflectionBackend.findCTor(source, modifiers, parameters);
        }
    }
}
