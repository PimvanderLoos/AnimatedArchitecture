package nl.pim16aap2.bigDoors.reflection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Represents a class that can find reflection objects in classes.
 *
 * @param <T> The type of objects to retrieve using reflection (e.g. {@link Field}, {@link Method}, etc).
 * @param <U> The type of finder being used to find the reflection object.
 * @author Pim
 */
@SuppressWarnings({"unused", "unchecked"})
public abstract class ReflectionFinder<T, U extends ReflectionFinder<T, U>>
{
    protected boolean nonnull = true;
    protected int modifiers = 0;

    protected ReflectionFinder()
    {
    }

    // Copy constructor
    protected ReflectionFinder(@NotNull ReflectionFinder<?, ?> other)
    {
        this.nonnull = (Objects.requireNonNull(other, "Copy constructor cannot copy from null object!")).nonnull;
        this.modifiers = other.modifiers;
    }

    /**
     * Retrieves the target reflection object(s) using the current configuration.
     * <p>
     * This may throw an exception if the lookup hasn't been configured yet.
     *
     * @return The target reflection object(s) being searched for. If no object(s) could be found and {@link
     * #setNullable()} is set, null will be returned.
     *
     * @throws NullPointerException When {@link #setNonnull()} is set (default).
     */
    public abstract T get();

    /**
     * Configures the lookup operation to allow returning null values.
     *
     * @return The current finder instance.
     */
    public @NotNull U setNullable()
    {
        nonnull = false;
        return (U) this;
    }

    /**
     * Configures the lookup operation to not allow returning null values.
     * <p>
     * When the lookup operation is unable to find a match, a {@link NullPointerException} will be thrown instead.
     *
     * @return The current finder instance.
     */
    public @NotNull U setNonnull()
    {
        nonnull = true;
        return (U) this;
    }

    /**
     * See {@link #addModifiers(int...)}.
     * <p>
     * Setting this to 0 will cause the finder to ignore the modifiers of the lookup object altogether (default).
     *
     * @param mods The set of modifiers to use as search constraint.
     * @return The current finder instance.
     */
    public @NotNull U withModifiers(int... mods)
    {
        this.modifiers = ReflectionUtils.getModifiers(mods);
        return (U) this;
    }

    /**
     * Adds a set of modifiers to the current set of modifiers.
     * <p>
     * The modifiers are used as search constraint, where the modifiers of the target object this finder is looking for
     * have to be an exact match to the configured modifiers. This means that this will have to be an exact match, so a
     * "public final" field cannot be found if the modifiers are set to just "public".
     *
     * @param mods The set of modifiers to use add to the current set of modifiers.
     * @return The current finder instance.
     */
    public @NotNull U addModifiers(int... mods)
    {
        for (int mod : mods)
            this.modifiers |= mod;
        return (U) this;
    }

    /**
     * Represents a type of {@link ReflectionFinder} that also has a group of parameters.
     * <p>
     * This can be useful for things like methods.
     *
     * @param <T> The type of objects to retrieve using reflection (e.g. {@link Field}, {@link Method}, etc).
     * @param <U> The type of finder being used to find the reflection object.
     */
    public abstract static class ReflectionFinderWithParameters<T, U extends ReflectionFinderWithParameters<T, U>>
        extends ReflectionFinder<T, U>
    {
        protected @Nullable ParameterGroup parameters;

        protected ReflectionFinderWithParameters()
        {
        }

        // Copy constructor
        protected ReflectionFinderWithParameters(@NotNull ReflectionFinderWithParameters<?, ?> other)
        {
            super(other);
            this.parameters = other.parameters == null ? null : new ParameterGroup(other.parameters);
        }

        /**
         * Indicates that the target object being searched for takes exactly 0 parameters.
         * <p>
         * If you want to ignore parameters altogether (default), use {@link #ignoreParameters()} instead.
         *
         * @return The current finder instance.
         */
        public @NotNull U withoutParameters()
        {
            this.parameters = new ParameterGroup();
            return (U) this;
        }

        /**
         * Indicates that the current finder should not take the parameters of the target object being searched for into
         * account. This is the default behavior.
         *
         * @return The current finder instance.
         */
        public @NotNull U ignoreParameters()
        {
            this.parameters = null;
            return (U) this;
        }

        /**
         * Indicates the set of required arguments this finder should consider when looking for the target object being
         * searched for.
         * <p>
         * If optional parameters are also required, {@link #withParameters(ParameterGroup)} should be used instead.
         *
         * @param parameters The required parameters of the target object.
         * @return The current finder instance.
         */
        public @NotNull U withParameters(@NotNull Class<?>... parameters)
        {
            this.parameters = new ParameterGroup().withRequiredParameters(parameters);
            return (U) this;
        }

        /**
         * Indicates the set of required arguments this finder should consider when looking for the target object being
         * searched for.
         *
         * @param parameters The required parameters of the target object.
         * @return The current finder instance.
         */
        public @NotNull U withParameters(@NotNull ParameterGroup parameters)
        {
            this.parameters = parameters;
            return (U) this;
        }
    }
}
