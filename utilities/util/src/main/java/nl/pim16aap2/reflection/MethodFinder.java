package nl.pim16aap2.reflection;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
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
     * @param source
     *     The class to analyze.
     * @return The next step in the method finding process.
     */
    @CheckReturnValue @Contract(pure = true)
    public MethodFinderInSource inClass(Class<?> source)
    {
        return new MethodFinderInSource(Objects.requireNonNull(source, "Source class cannot be null!"));
    }

    /**
     * Represents the second step of the method finder, after the source class has been specified.
     */
    public static final class MethodFinderInSource
    {
        private final Class<?> source;

        private MethodFinderInSource(Class<?> source)
        {
            this.source = source;
        }

        /**
         * Creates a new {@link MethodFinderBase} to retrieve the method by its name.
         *
         * @param name
         *     The name of the method to look for.
         * @return The new {@link MethodFinderBase}.
         */
        @CheckReturnValue @Contract(pure = true)
        public MethodFinderBase withName(String name)
        {
            return new NamedMethodFinder(source, name);
        }

        /**
         * Creates a new {@link MethodFinderBase} to retrieve the method by its type.
         *
         * @param returnType
         *     The return type of the method to look for.
         * @return The new {@link MethodFinderBase}.
         */
        @CheckReturnValue @Contract(pure = true)
        public MethodFinderBase withReturnType(Class<?> returnType)
        {
            return new TypedMethodFinder(source, returnType);
        }
    }

    /**
     * Represents an implementation of {@link ReflectionFinderWithParameters} that can be used to find a method.
     */
    public abstract static class MethodFinderBase
        extends ReflectionFinder.ReflectionFinderWithParameters<Method, MethodFinderBase>
        implements IAccessibleSetter<MethodFinderBase>, IAnnotationFinder<MethodFinderBase>
    {
        protected final Class<?> source;
        protected boolean checkSuperClasses = false;
        protected boolean checkInterfaces = false;
        protected boolean setAccessible = false;
        @SuppressWarnings("unchecked")
        protected Class<? extends Annotation>[] annotations = new Class[0];

        private MethodFinderBase(Class<?> source)
        {
            this.source = Objects.requireNonNull(source, "Source class has not been set yet!");
        }

        // Copy constructor
        private MethodFinderBase(MethodFinderBase other)
        {
            super(other);
            source = other.source;
            checkSuperClasses = other.checkSuperClasses;
            checkInterfaces = other.checkInterfaces;
            setAccessible = other.setAccessible;
            annotations = other.annotations;
        }

        @Override
        @SafeVarargs
        public final MethodFinderBase withAnnotations(Class<? extends Annotation>... annotations)
        {
            this.annotations = annotations;
            return this;
        }

        @Override
        public MethodFinderBase setAccessible()
        {
            setAccessible = true;
            return this;
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
        public MethodFinderBase checkSuperClasses()
        {
            checkSuperClasses = true;
            return this;
        }

        /**
         * Used to configure the method finder to ignore the {@link #source} class's superclasses when searching for the
         * target method. This is the default behavior.
         *
         * @return The current method finder instance.
         */
        public MethodFinderBase ignoreSuperClasses()
        {
            checkSuperClasses = false;
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
        public MethodFinderBase checkInterfaces()
        {
            checkInterfaces = true;
            return this;
        }

        /**
         * Used to configure the method finder to ignore the {@link #source} class's interfaces when searching for the
         * target method. This is the default behavior.
         *
         * @return The current method finder instance.
         */
        public MethodFinderBase ignoreInterfaces()
        {
            checkInterfaces = false;
            return this;
        }

        protected final String getNullErrorMessage(@Nullable String name, @Nullable Class<?> returnType)
        {
            return String.format("Failed to find method: [%s %s %s#%s(%s)]. Super classes were %s.",
                                 ReflectionBackend.optionalModifiersToString(modifiers),
                                 ReflectionBackend.formatOptionalValue(returnType, Class::getName),
                                 source.getName(), ReflectionBackend.formatOptionalValue(null),
                                 ReflectionBackend.formatOptionalValue(parameters),
                                 checkSuperClasses ? "included" : "excluded");
        }
    }

    /**
     * Represents an implementation of {@link MethodFinderBase} to retrieve a method by its name.
     */
    private static final class NamedMethodFinder extends MethodFinderBase
    {
        private final String name;

        private NamedMethodFinder(Class<?> source, String name)
        {
            super(source);
            this.name = Objects.requireNonNull(name, "Name of named method cannot be null!");
        }

        @Override
        public Method get()
        {
            //noinspection ConstantConditions
            return Objects.requireNonNull(getNullable(), getNullErrorMessage(name, null));
        }

        @Override
        public @Nullable Method getNullable()
        {
            return ReflectionBackend.findMethod(
                checkSuperClasses, checkInterfaces, source, name, modifiers, parameters, null, setAccessible);
        }
    }

    /**
     * Represents an implementation of {@link MethodFinderBase} to retrieve a method by its return type.
     */
    private static final class TypedMethodFinder extends MethodFinderBase
    {
        private final Class<?> returnType;

        private TypedMethodFinder(Class<?> source, Class<?> returnType)
        {
            super(source);
            this.returnType = Objects.requireNonNull(returnType, "Return type of typed method cannot be null!");
        }

        @Override
        public Method get()
        {
            //noinspection ConstantConditions
            return Objects.requireNonNull(getNullable(), getNullErrorMessage(null, returnType));
        }

        @Override
        public @Nullable Method getNullable()
        {
            return ReflectionBackend.findMethod(
                checkSuperClasses, checkInterfaces, source, null, modifiers, parameters, returnType, setAccessible);
        }
    }
}
