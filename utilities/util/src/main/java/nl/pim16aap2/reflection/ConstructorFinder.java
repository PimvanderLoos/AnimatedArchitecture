package nl.pim16aap2.reflection;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
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
        implements IAccessibleSetter<ConstructorFinderInSource>, IAnnotationFinder<ConstructorFinderInSource>
    {
        private final Class<?> source;

        private boolean setAccessible = false;

        @SuppressWarnings("unchecked")
        private Class<? extends Annotation>[] annotations = new Class[0];

        private ConstructorFinderInSource(Class<?> source)
        {
            this.source = source;
        }

        @Override
        public Constructor<?> get()
        {
            return Objects.requireNonNull(getNullable(), String.format(
                "Failed to find constructor %s[%s %s(%s)]",
                ReflectionBackend.formatAnnotations(annotations),
                ReflectionBackend.optionalModifiersToString(modifiers),
                source.getName(),
                ReflectionBackend.formatOptionalValue(parameters)));
        }

        /**
         * @return All constructors that match the provided data.
         */
        public List<Constructor<?>> getAll()
        {
            return ReflectionBackend.findCTor(
                source, modifiers, parameters, setAccessible, Integer.MAX_VALUE, annotations);
        }

        @Override
        public @Nullable Constructor<?> getNullable()
        {
            final List<Constructor<?>> ctors =
                ReflectionBackend.findCTor(source, modifiers, parameters, setAccessible, 1, annotations);
            return ctors.isEmpty() ? null : ctors.get(0);
        }

        @Override
        public ConstructorFinderInSource setAccessible()
        {
            setAccessible = true;
            return this;
        }

        @Override
        @SafeVarargs
        public final ConstructorFinderInSource withAnnotations(Class<? extends Annotation>... annotations)
        {
            this.annotations = annotations;
            return this;
        }
    }
}
