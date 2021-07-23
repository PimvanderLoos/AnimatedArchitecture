package nl.pim16aap2.bigDoors.reflection;

import org.jetbrains.annotations.NotNull;

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
     * @param source The class to analyze.
     * @return The next step in the constructor finding process.
     */
    public @NotNull ConstructorFinderInSource inClass(@NotNull Class<?> source)
    {
        return new ConstructorFinderInSource(Objects.requireNonNull(source, "Source class cannot be null!"));
    }

    /**
     * Represents an implementation of {@link ReflectionFinder}
     */
    public static final class ConstructorFinderInSource
        extends ReflectionFinder.ReflectionFinderWithParameters<Constructor<?>, ConstructorFinderInSource>
    {
        private final @NotNull Class<?> source;

        private ConstructorFinderInSource(@NotNull Class<?> source)
        {
            this.source = source;
        }

        @Override
        public Constructor<?> get()
        {
            return ReflectionBackend.findCTor(nonnull, source, modifiers, parameters);
        }
    }
}
