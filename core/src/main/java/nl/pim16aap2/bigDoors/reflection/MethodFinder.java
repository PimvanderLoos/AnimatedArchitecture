package nl.pim16aap2.bigDoors.reflection;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * @param source
     *     The class to analyze.
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
    public final class MethodFinderInSource
    {
        private final @NotNull Class<?> source;

        private MethodFinderInSource(@NotNull Class<?> source)
        {
            this.source = source;
        }

        /**
         * Creates a new {@link NamedMethodFinder} to retrieve the method by its name.
         *
         * @param name
         *     The name of the method to look for.
         * @return The new {@link NamedMethodFinder}.
         */
        @Contract("_ -> new")
        public NamedMethodFinder withName(@NotNull String name)
        {
            return new NamedMethodFinder(source, name);
        }

        /**
         * Creates a new {@link TypedMethodFinder} to retrieve the method by its type.
         *
         * @param returnType
         *     The return type of the method to look for.
         * @return The new {@link TypedMethodFinder}.
         */
        @Contract("_ -> new")
        public TypedMethodFinder withReturnType(@NotNull Class<?> returnType)
        {
            return new TypedMethodFinder(source, returnType);
        }

        /**
         * Creates a new {@link MultipleMethodsFinder} to retrieve multiple methods that match the given input.
         *
         * @return The new {@link MultipleMethodsFinder}.
         */
        public MultipleMethodsFinder findMultiple()
        {
            return new MultipleMethodsFinder(source);
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinderWithParameters} that can be used to find a method.
     */
    @SuppressWarnings("unchecked")
    public abstract static class MethodFinderBase<T, U extends MethodFinderBase<T, U>>
        extends ReflectionFinder.ReflectionFinderWithParameters<T, U>
    {
        protected final @NotNull Class<?> source;
        protected boolean checkSuperClasses = false;
        protected boolean checkInterfaces = false;

        private MethodFinderBase(Class<?> source)
        {
            this.source = Objects.requireNonNull(source, "Source class has not been set yet!");
        }

        // Copy constructor
        private MethodFinderBase(@NotNull MethodFinderBase<T, U> other)
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
        public U checkSuperClasses()
        {
            this.checkSuperClasses = true;
            return (U) this;
        }

        /**
         * Used to configure the method finder to ignore the {@link #source} class's superclasses when searching for the
         * target method. This is the default behavior.
         *
         * @return The current method finder instance.
         */
        @Contract("-> this")
        public U ignoreSuperClasses()
        {
            this.checkSuperClasses = false;
            return (U) this;
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
        public U checkInterfaces()
        {
            this.checkInterfaces = true;
            return (U) this;
        }

        /**
         * Used to configure the method finder to ignore the {@link #source} class's interfaces when searching for the
         * target method. This is the default behavior.
         *
         * @return The current method finder instance.
         */
        @Contract("-> this")
        public U ignoreInterfaces()
        {
            this.checkInterfaces = false;
            return (U) this;
        }
    }

    /**
     * Represents an implementation of {@link MethodFinderBase} to retrieve a method by its name.
     */
    public static final class NamedMethodFinder extends MethodFinderBase<Method, NamedMethodFinder>
    {
        private final @NotNull String name;

        private NamedMethodFinder(Class<?> source, @NotNull String name)
        {
            super(source);
            this.name = Objects.requireNonNull(name, "Name of named method cannot be null!");
        }

        /**
         * Returns the selected if exactly 1 could be found. If zero or >1 methods were found matching the input,
         * either null is returned or a {@link NullPointerException} is thrown.
         *
         * @inheritDoc
         */
        @Override
        public Method get()
        {
            return ReflectionBackend.findMethod(nonnull, checkSuperClasses, checkInterfaces,
                                                source, name, modifiers, parameters, null);
        }
    }

    /**
     * Represents an implementation of {@link MethodFinderBase} to retrieve a method by its return type.
     */
    public static final class TypedMethodFinder extends MethodFinderBase<Method, TypedMethodFinder>
    {
        private final @NotNull Class<?> returnType;

        private TypedMethodFinder(Class<?> source, @NotNull Class<?> returnType)
        {
            super(source);
            this.returnType = Objects.requireNonNull(returnType, "Return type of typed method cannot be null!");
        }

        /**
         * Returns the selected if exactly 1 could be found. If zero or >1 methods were found matching the input,
         * either null is returned or a {@link NullPointerException} is thrown.
         *
         * @inheritDoc
         */
        @Override
        public Method get()
        {
            return ReflectionBackend.findMethod(nonnull, checkSuperClasses, checkInterfaces,
                                                source, null, modifiers, parameters, returnType);
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinder} that is used to find multiple methods that all fit the
     * provided details.
     */
    public final class MultipleMethodsFinder extends MethodFinderBase<List<Method>, MultipleMethodsFinder>
        implements IBoundedRetriever<List<Method>, MultipleMethodsFinder>
    {
        private @Nullable String name;
        private @Nullable Class<?> returnType;
        private int expected = -1;
        private int atMost = -1;
        private int atLeast = -1;

        private MultipleMethodsFinder(Class<?> source)
        {
            super(source);
        }

        private MultipleMethodsFinder(MultipleMethodsFinder other)
        {
            super(other);
            this.name = other.name;
            this.returnType = other.returnType;
            this.expected = other.expected;
            this.atMost = other.atMost;
            this.atLeast = other.atLeast;
        }

        /**
         * Specifies the name of the methods to look for.
         *
         * @param name
         *     The name of the methods to look for.
         * @return The current {@link MultipleMethodsFinder}.
         */
        @Contract("_ -> this")
        public MultipleMethodsFinder withName(@NotNull String name)
        {
            this.name = name;
            return this;
        }

        /**
         * Specifies the return type of the methods to look for.
         *
         * @param returnType
         *     The return type of the methods to look for.
         * @return The current {@link MultipleMethodsFinder}.
         */
        @Contract("_ -> this")
        public MultipleMethodsFinder withReturnType(@NotNull Class<?> returnType)
        {
            this.returnType = returnType;
            return this;
        }

        @Override
        public List<Method> get()
        {
            final List<Method> found =
                ReflectionBackend.findMethods(checkSuperClasses, checkInterfaces, source,
                                              name, modifiers, parameters, returnType);

            if (expected >= 0 && expected != found.size())
                return handleInvalid(
                    String.format("Expected %d methods but found %d for input: ", expected, found.size()));

            if (atMost >= 0 && found.size() > atMost)
                return handleInvalid(
                    String.format("Expected at most %d methods, but found %d for input: ", atMost, found.size()));

            if (atLeast >= 0 && found.size() < atLeast)
                return handleInvalid(
                    String.format("Expected at least %d methods, but found %d for input: ", atLeast, found.size()));
            return found;
        }

        private List<Method> handleInvalid(@NotNull String str)
        {
            if (nonnull)
                throw new IllegalStateException(str + ReflectionBackend
                    .methodSearchRequestToString(checkSuperClasses, checkInterfaces, source, name,
                                                 modifiers, parameters, returnType));
            return null;
        }

        @Override
        public MultipleMethodsFinder atLeast(int val)
        {
            this.atLeast = val;
            return this;
        }

        @Override
        public MultipleMethodsFinder atMost(int val)
        {
            this.atMost = val;
            return this;
        }

        @Override
        public MultipleMethodsFinder exactCount(int val)
        {
            this.expected = val;
            return this;
        }
    }
}
