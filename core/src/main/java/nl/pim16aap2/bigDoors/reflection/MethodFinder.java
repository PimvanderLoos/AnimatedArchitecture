package nl.pim16aap2.bigDoors.reflection;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Represents a {@link ReflectionFinder} for methods.
 * <p>
 * This class can be used to retrieve methods by name and by return type.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public final class MethodFinder
{
    /**
     * Sets the class this method finder will search in for methods.
     *
     * @param source The class to analyze.
     * @return The next step in the method finding process.
     */
    public @NotNull MethodFinderInSource inClass(@NotNull Class<?> source)
    {
        return new MethodFinderInSource(Objects.requireNonNull(source, "Source class cannot be null!"));
    }

    /**
     * Represents the second step of the method finder, after the source class has been specified.
     */
    public static final class MethodFinderInSource
    {
        private final @NotNull Class<?> source;

        private MethodFinderInSource(@NotNull Class<?> source)
        {
            this.source = source;
        }

        /**
         * Creates a new {@link MethodFinderBase} to retrieve the method by its name.
         *
         * @param name The name of the method to look for.
         * @return The new {@link MethodFinderBase}.
         */
        public @NotNull MethodFinderBase withName(@NotNull String name)
        {
            return new NamedMethodFinder(source, name);
        }

        /**
         * Creates a new {@link MethodFinderBase} to retrieve the method by its type.
         *
         * @param returnType The return type of the method to look for.
         * @return The new {@link MethodFinderBase}.
         */
        public @NotNull MethodFinderBase withReturnType(@NotNull Class<?> returnType)
        {
            return new TypedMethodFinder(source, returnType);
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinderWithParameters} that can be used to find a method.
     */
    public abstract static class MethodFinderBase
        extends ReflectionFinder.ReflectionFinderWithParameters<Method, MethodFinderBase>
    {
        protected final @NotNull Class<?> source;
        protected boolean checkSuperClasses = false;

        private MethodFinderBase(Class<?> source)
        {
            this.source = Objects.requireNonNull(source, "Source class has not been set yet!");
        }

        // Copy constructor
        private MethodFinderBase(@NotNull MethodFinderBase other)
        {
            super(other);
            this.source = other.source;
            this.checkSuperClasses = other.checkSuperClasses;
        }

        /**
         * Used to configure the method finder to include the {@link #source} class's superclasses when searching for
         * the target method.
         *
         * @return The current method finder instance.
         */
        public @NotNull MethodFinderBase checkSuperClasses()
        {
            this.checkSuperClasses = true;
            return this;
        }

        /**
         * Used to configure the method finder to ignore the {@link #source} class's superclasses when searching for the
         * target method. This is the default behavior.
         *
         * @return The current method finder instance.
         */
        public @NotNull MethodFinderBase ignoreClasses()
        {
            this.checkSuperClasses = false;
            return this;
        }
    }

    /**
     * Represents an implementation of {@link MethodFinderBase} to retrieve a method by its name.
     */
    private static final class NamedMethodFinder extends MethodFinderBase
    {
        private final @NotNull String name;

        private NamedMethodFinder(Class<?> source, @NotNull String name)
        {
            super(source);
            this.name = Objects.requireNonNull(name, "Name of named method cannot be null!");
        }

        @Override
        public Method get()
        {
            return ReflectionBackend.findMethod(nonnull, checkSuperClasses, source, name, modifiers, parameters, null);
        }
    }

    /**
     * Represents an implementation of {@link MethodFinderBase} to retrieve a method by its return type.
     */
    private static final class TypedMethodFinder extends MethodFinderBase
    {
        private final @NotNull Class<?> returnType;

        private TypedMethodFinder(Class<?> source, @NotNull Class<?> returnType)
        {
            super(source);
            this.returnType = Objects.requireNonNull(returnType, "Return type of typed method cannot be null!");
        }

        @Override
        public Method get()
        {
            return ReflectionBackend
                .findMethod(nonnull, checkSuperClasses, source, null, modifiers, parameters, returnType);
        }
    }
}
