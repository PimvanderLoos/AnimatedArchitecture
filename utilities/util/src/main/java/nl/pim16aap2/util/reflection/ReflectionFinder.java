package nl.pim16aap2.util.reflection;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Represents a class that can find reflection objects in classes.
 *
 * @param <T>
 *     The type of objects to retrieve using reflection (e.g. {@link Field}, {@link Method}, etc.)
 * @param <U>
 *     The type of finder being used to find the reflection object.
 * @author Pim
 */
@SuppressWarnings({"unused", "unchecked"})
public abstract class ReflectionFinder<T, U extends ReflectionFinder<T, U>>
{
    protected int modifiers = 0;

    protected ReflectionFinder()
    {
    }

    // Copy constructor
    protected ReflectionFinder(ReflectionFinder<?, ?> other)
    {
        modifiers = Objects.requireNonNull(other, "Copy constructor cannot copy from null object!").modifiers;
    }

    /**
     * Retrieves the target reflection object(s) using the current configuration.
     * <p>
     * This may throw an exception if the lookup hasn't been configured yet.
     *
     * @return The target reflection object(s) being searched for.
     *
     * @throws NullPointerException
     *     If the target reflection object(s) could not be found.
     */
    @CheckReturnValue @Contract(pure = true)
    public abstract T get();

    /**
     * Retrieves the target reflection object(s) using the current configuration.
     * <p>
     * This may throw an exception if the lookup hasn't been configured yet.
     *
     * @return The target reflection object(s) being searched for or null if no object(s) could be found.
     */
    @CheckReturnValue @Contract(pure = true)
    public abstract @Nullable T getNullable();

    /**
     * See {@link #addModifiers(int...)}.
     * <p>
     * Setting this to 0 will cause the finder to ignore the modifiers of the lookup object altogether (default).
     *
     * @param mods
     *     The set of modifiers to use as search constraint.
     * @return The current finder instance.
     */
    public U withModifiers(int... mods)
    {
        modifiers = ReflectionBackend.getModifiers(mods);
        return (U) this;
    }

    /**
     * Adds a set of modifiers to the current set of modifiers.
     * <p>
     * The modifiers are used as search constraint, where the modifiers of the target object this finder is looking for
     * have to be an exact match to the configured modifiers. This means that this will have to be an exact match, so a
     * "public final" field cannot be found if the modifiers are set to just "public".
     *
     * @param mods
     *     The set of modifiers to use add to the current set of modifiers.
     * @return The current finder instance.
     */
    public U addModifiers(int... mods)
    {
        for (final int mod : mods)
            modifiers |= mod;
        return (U) this;
    }

    /**
     * Represents a type of {@link ReflectionFinder} that also has a group of parameters.
     * <p>
     * This can be useful for things like methods.
     *
     * @param <T>
     *     The type of objects to retrieve using reflection (e.g. {@link Field}, {@link Method}, etc.)
     * @param <U>
     *     The type of finder being used to find the reflection object.
     */
    public abstract static class ReflectionFinderWithParameters<T, U extends ReflectionFinderWithParameters<T, U>>
        extends ReflectionFinder<T, U>
    {
        protected @Nullable ParameterGroup parameters;

        protected ReflectionFinderWithParameters()
        {
        }

        // Copy constructor
        protected ReflectionFinderWithParameters(ReflectionFinderWithParameters<?, ?> other)
        {
            super(other);
            parameters = other.parameters == null ? null : new ParameterGroup(other.parameters);
        }

        /**
         * Indicates that the target object being searched for takes exactly 0 parameters.
         * <p>
         * If you want to ignore parameters altogether (default), use {@link #ignoreParameters()} instead.
         *
         * @return The current finder instance.
         */
        public U withoutParameters()
        {
            parameters = new ParameterGroup.Builder().construct();
            return (U) this;
        }

        /**
         * Indicates that the current finder should not take the parameters of the target object being searched for into
         * account. This is the default behavior.
         *
         * @return The current finder instance.
         */
        public U ignoreParameters()
        {
            parameters = null;
            return (U) this;
        }

        /**
         * Indicates the set of required arguments this finder should consider when looking for the target object being
         * searched for.
         * <p>
         * If optional parameters are also required, {@link #withParameters(ParameterGroup)} should be used instead.
         *
         * @param parameters
         *     The required parameters of the target object.
         * @return The current finder instance.
         */
        public U withParameters(Class<?>... parameters)
        {
            this.parameters = new ParameterGroup.Builder().withRequiredParameters(parameters).construct();
            return (U) this;
        }

        /**
         * Indicates the set of required arguments this finder should consider when looking for the target object being
         * searched for.
         *
         * @param parameters
         *     The parameters of the target object.
         * @return The current finder instance.
         */
        public U withParameters(ParameterGroup parameters)
        {
            this.parameters = parameters;
            return (U) this;
        }

        /**
         * Indicates the set of required arguments this finder should consider when looking for the target object being
         * searched for.
         *
         * @param parameters
         *     The parameters of the target object.
         * @return The current finder instance.
         */
        public U withParameters(ParameterGroup.Builder parameters)
        {
            this.parameters = parameters.construct();
            return (U) this;
        }
    }
}
