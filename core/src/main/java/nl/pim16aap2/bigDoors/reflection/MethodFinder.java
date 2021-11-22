package nl.pim16aap2.bigDoors.reflection;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
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
    @Contract("_ -> new")
    public MethodFinderInSource inClass(@NotNull Class<?> source)
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
        @Contract("_ -> new")
        public MethodFinderBase withName(@NotNull String name)
        {
            return new NamedMethodFinder(source, name);
        }

        /**
         * Creates a new {@link MethodFinderBase} to retrieve the method by its type.
         *
         * @param returnType The return type of the method to look for.
         * @return The new {@link MethodFinderBase}.
         */
        @Contract("_ -> new")
        public MethodFinderBase withReturnType(@NotNull Class<?> returnType)
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
        protected boolean checkInterfaces = false;

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
            this.checkInterfaces = other.checkInterfaces;
        }

        /**
         * Used to configure the method finder to include the {@link #source} class's superclasses when searching for
         * the target method.
         * <p>
         * Note that when both {@link #checkSuperClasses} and {@link #checkInterfaces} are enabled, the superclasses
         * will take precedence. This means that all super classes (and their interfaces) are evaluated before the
         * interfaces of the source class are evaluated.
         *
         * @return The current method finder instance.
         */
        @Contract("-> this")
        public MethodFinderBase checkSuperClasses()
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
        @Contract("-> this")
        public MethodFinderBase ignoreSuperClasses()
        {
            this.checkSuperClasses = false;
            return this;
        }

        /**
         * Used to configure the method finder to include the {@link #source} class's interfaces when searching for the
         * target method.
         * <p>
         * Note that when both {@link #checkSuperClasses} and {@link #checkInterfaces} are enabled, the superclasses
         * will take precedence. This means that all super classes (and their interfaces) are evaluated before the
         * interfaces of the source class are evaluated.
         *
         * @return The current method finder instance.
         */
        @Contract("-> this")
        public MethodFinderBase checkInterfaces()
        {
            this.checkInterfaces = true;
            return this;
        }

        /**
         * Used to configure the method finder to ignore the {@link #source} class's interfaces when searching for the
         * target method. This is the default behavior.
         *
         * @return The current method finder instance.
         */
        @Contract("-> this")
        public MethodFinderBase ignoreInterfaces()
        {
            this.checkInterfaces = false;
            return this;
        }

        /**
         * Gets all methods that fit the provided signature.
         *
         * @param expected The exact number of methods that should be found.
         * @return The methods that were found.
         * @throws IllegalStateException when the number of found methods does not match the expected number of methods.
         */
        public abstract List<Method> get(int expected);

        /**
         * Retrieves all methods that fit the provided signature.
         *
         * @return A list with all methods that fit the provided signature.
         */
        public abstract List<Method> getAll();
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
            return ReflectionBackend.findMethod(nonnull, checkSuperClasses, checkInterfaces,
                                                source, name, modifiers, parameters, null);
        }

        public List<Method> get(int expected)
        {
            final List<Method> ret = getAll();
            if (ret.size() != expected)
                throw new IllegalStateException("Expected " + expected + " methods, but found " + ret.size());
            return ret;
        }

        public List<Method> getAll()
        {
            return ReflectionBackend.findMethods(checkSuperClasses, checkInterfaces, source,
                                                 name, modifiers, parameters, null, Integer.MAX_VALUE);
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
            return ReflectionBackend.findMethod(nonnull, checkSuperClasses, checkInterfaces,
                                                source, null, modifiers, parameters, returnType);
        }

        public List<Method> get(int expected)
        {
            final List<Method> ret = getAll();
            if (ret.size() != expected)
                throw new IllegalStateException("Expected " + expected + " methods, but found " + ret.size());
            return ret;
        }

        public List<Method> getAll()
        {
            return ReflectionBackend.findMethods(checkSuperClasses, checkInterfaces, source, null,
                                                 modifiers, parameters, returnType, Integer.MAX_VALUE);
        }
    }
}
