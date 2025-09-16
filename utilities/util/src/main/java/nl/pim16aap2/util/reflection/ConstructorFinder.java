package nl.pim16aap2.util.reflection;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;

/**
 * Represents a {@link ReflectionFinder} that can find {@link Constructor}s.
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
    @CheckReturnValue
    @Contract(pure = true)
    public <T> ConstructorFinderInSource<T> inClass(Class<T> source)
    {
        return new ConstructorFinderInSource<>(Objects.requireNonNull(source, "Source class cannot be null!"));
    }

    /**
     * Represents an implementation of {@link ReflectionFinder}
     */
    public static final class ConstructorFinderInSource<T>
        extends ReflectionFinder.ReflectionFinderWithParameters<Constructor<T>, ConstructorFinderInSource<T>>
        implements IAccessibleSetter<ConstructorFinderInSource<T>>, IAnnotationFinder<ConstructorFinderInSource<T>>
    {
        private final Class<T> source;

        private boolean setAccessible = false;

        @SuppressWarnings("unchecked")
        private Class<? extends Annotation>[] annotations = new Class[0];

        private ConstructorFinderInSource(Class<T> source)
        {
            this.source = source;
        }

        @Override
        public Constructor<T> get()
        {
            return Objects.requireNonNull(getNullable(), this::toString);
        }

        /**
         * Gets all constructors that match the provided data.
         *
         * @return All constructors that match the provided data.
         */
        public List<Constructor<T>> getAll()
        {
            return ReflectionBackend.findCTor(
                source,
                modifiers,
                parameters,
                setAccessible,
                Integer.MAX_VALUE,
                annotations
            );
        }

        @Override
        public @Nullable Constructor<T> getNullable()
        {
            try
            {
                final List<Constructor<T>> ctors =
                    ReflectionBackend.findCTor(
                        source,
                        modifiers,
                        parameters,
                        setAccessible,
                        1,
                        annotations
                    );
                return ctors.isEmpty() ? null : ctors.getFirst();
            }
            catch (Throwable throwable)
            {
                throw new RuntimeException("Failed to find constructors for request: " + this, throwable);
            }
        }

        @Override
        public ConstructorFinderInSource<T> setAccessible()
        {
            setAccessible = true;
            return this;
        }

        @Override
        @SafeVarargs
        public final ConstructorFinderInSource<T> withAnnotations(Class<? extends Annotation>... annotations)
        {
            this.annotations = annotations;
            return this;
        }

        @Override
        public String toString()
        {
            return String.format(
                "Failed to find constructor %s[%s %s(%s)]",
                ReflectionBackend.formatAnnotations(annotations),
                ReflectionBackend.optionalModifiersToString(modifiers),
                source.getName(),
                ReflectionBackend.formatOptionalValue(parameters)
            );
        }
    }
}
