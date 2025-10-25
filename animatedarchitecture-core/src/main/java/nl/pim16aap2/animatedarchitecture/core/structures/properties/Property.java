package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import com.google.errorprone.annotations.CheckReturnValue;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IKeyed;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.RedstoneMode;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a property of a structure.
 * <p>
 * Several properties are predefined in this class. For example, the {@link #OPEN_STATUS} property is used to represent
 * whether a structure is open or closed.
 *
 * @param <T>
 *     The type of the value of the property.
 */
@ToString
@EqualsAndHashCode
@CustomLog
public final class Property<T> implements IKeyed
{
    /**
     * The registry of all registered properties.
     */
    private static final Registry REGISTRY = new Registry();

    /**
     * A property for structures whose animation speed is variable.
     */
    public static final Property<Double> ANIMATION_SPEED_MULTIPLIER =
        builder("ANIMATION_SPEED_MULTIPLIER", Double.class)
            .withDefaultValue(1.0D)
            // I am not sure if the animator currently uses this property.
            .withoutUserOrAdminAccess()
            .build();

    /**
     * A property for structures that move a certain amount of blocks when activated.
     */
    public static final Property<Integer> BLOCKS_TO_MOVE =
        builder("BLOCKS_TO_MOVE", Integer.class)
            .withDefaultValue(0)
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            // Changing the blocks to move may affect things like animation range.
            .withPropertyScopes(PropertyScope.ANIMATION)
            .build();

    /**
     * A property for structures that have a defined open and closed state.
     */
    public static final Property<Boolean> OPEN_STATUS =
        builder("OPEN_STATUS", Boolean.class)
            .withDefaultValue(false)
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            .withPropertyScopes(
                // The open status affects things like animation direction.
                PropertyScope.ANIMATION,
                // Changing the open status may affect the current redstone action.
                PropertyScope.REDSTONE)
            .build();

    /**
     * A property for structures that can rotate multiples of 90 degrees.
     */
    public static final Property<Integer> QUARTER_CIRCLES =
        builder("QUARTER_CIRCLES", Integer.class)
            .withDefaultValue(1)
            // Quarter circles are not yet fully supported.
            .withoutUserOrAdminAccess()
            // Changing the quarter circles may affect things like animation range.
            .withPropertyScopes(PropertyScope.ANIMATION)
            .build();

    /**
     * A property for structures that can have different redstone modes.
     */
    public static final Property<RedstoneMode> REDSTONE_MODE =
        builder("REDSTONE_MODE", RedstoneMode.class)
            .withDefaultValue(RedstoneMode.DEFAULT)
            // There are currently no implementations for the alternative redstone modes.
            .withoutUserOrAdminAccess()
            // Changing the redstone mode may affect the current redstone action.
            .withPropertyScopes(PropertyScope.REDSTONE)
            .build();

    /**
     * A property for structures that have a defined rotation point.
     */
    public static final Property<Vector3Di> ROTATION_POINT =
        builder("ROTATION_POINT", Vector3Di.class)
            .withDefaultValue(new Vector3Di(0, Integer.MAX_VALUE, 0))
            .withUserAccessLevels(PropertyAccessLevel.READ)
            .withAdminAccessLevels(PropertyAccessLevel.EDIT)
            // Changing the rotation point may affect things like animation range.
            .withPropertyScopes(PropertyScope.ANIMATION)
            .build();

    /**
     * The namespace and key of the property.
     * <p>
     * Note that this name should be unique for each property.
     *
     * @return The name of the property.
     */
    @Getter
    private final NamespacedKey namespacedKey;

    /**
     * The type of the property.
     *
     * @return The type of the property.
     */
    @Getter
    private final Class<T> type;

    /**
     * The default value of the property.
     *
     * @return The default value of the property.
     */
    @Getter
    private final T defaultValue;

    /**
     * The level of access users have to this property.
     * <p>
     * This is a bit flag whose values refer to the {@link PropertyAccessLevel} enum.
     */
    @Getter
    private final int userAccessLevel;

    /**
     * The level of access admins have to this property.
     * <p>
     * This is a bit flag whose values refer to the {@link PropertyAccessLevel} enum.
     */
    @Getter
    private final int adminAccessLevel;

    /**
     * The scopes in which this property is used.
     * <p>
     * This is used to prevent side effects of changing the property value. For example, clearing cached values related
     * to the property, such as the animation range when changing the 'blocks to move' property.
     */
    @Getter
    private final List<PropertyScope> propertyScopes;

    @VisibleForTesting
    Property(
        NamespacedKey namespacedKey,
        Class<T> type,
        T defaultValue,
        int userAccessLevel,
        int adminAccessLevel,
        List<PropertyScope> scopes)
    {
        this.namespacedKey = Util.requireNonNull(namespacedKey, "NamespacedKey");
        this.type = Util.requireNonNull(type, "Property type");
        this.defaultValue = Util.requireNonNull(defaultValue, "Default Property value");
        this.propertyScopes = List.copyOf(scopes);

        this.userAccessLevel = userAccessLevel;
        this.adminAccessLevel = userAccessLevel | adminAccessLevel;

        if (!type.isInstance(defaultValue))
            throw new IllegalArgumentException("Default value " + defaultValue + " is not of type " + type.getName());

        REGISTRY.register(this);
    }

    /**
     * Creates a new property builder.
     * <p>
     * This is a shortcut for {@link #builder(NamespacedKey, Class)} with the owner set to
     * {@link Constants#PLUGIN_NAME}.
     *
     * @param name
     *     The name of the property that is used for serialization.
     * @param type
     *     The type of the property.
     * @param <U>
     *     The type of the value of the property.
     * @return The property builder.
     */
    private static <U> PropertyBuilder<U> builder(String name, Class<U> type)
    {
        return builder(Constants.PLUGIN_NAME, name, type);
    }

    /**
     * Creates a new property builder.
     * <p>
     * This is a shortcut for {@link #builder(NamespacedKey, Class)}.
     *
     * @param owner
     *     The name of the plugin that owns this property. This is used to prevent conflicts between properties between
     *     different plugins.
     *     <p>
     *     All properties supplied by this plugin are set to {@link Constants#PLUGIN_NAME}.
     *     <p>
     *     Properties from other plugins should use the name of the plugin that owns the property.
     * @param name
     *     The name of the property that is used for serialization.
     * @param type
     *     The type of the property.
     * @param <U>
     *     The type of the value of the property.
     * @return The property builder.
     */
    public static <U> PropertyBuilder<U> builder(String owner, String name, Class<U> type)
    {
        return builder(new NamespacedKey(owner, name), type);
    }

    /**
     * Creates a new property builder.
     *
     * @param namespacedKey
     *     The namespace and key of the property.
     * @param type
     *     The type of the property.
     * @param <U>
     *     The type of the value of the property.
     * @return The property builder.
     */
    public static <U> PropertyBuilder<U> builder(NamespacedKey namespacedKey, Class<U> type)
    {
        return new PropertyBuilder<>(namespacedKey, type);
    }

    /**
     * Gets the debuggable registry of all registered properties.
     *
     * @return The debuggable registry of all registered properties.
     */
    public static IDebuggable getDebuggableRegistry()
    {
        return REGISTRY;
    }

    /**
     * Checks if the user has the given access level.
     *
     * @param propertyAccessLevel
     *     The access level to check.
     * @return True if the user has the given access level.
     */
    public boolean userHasAccessLevel(PropertyAccessLevel propertyAccessLevel)
    {
        return PropertyAccessLevel.hasFlag(userAccessLevel, propertyAccessLevel);
    }

    /**
     * Checks if the admin has the given access level.
     *
     * @param propertyAccessLevel
     *     The access level to check.
     * @return True if the admin has the given access level.
     */
    public boolean adminHasAccessLevel(PropertyAccessLevel propertyAccessLevel)
    {
        return PropertyAccessLevel.hasFlag(adminAccessLevel, propertyAccessLevel);
    }

    /**
     * Checks whether this property grants the specified access level to the given permission level.
     * <p>
     * This method retrieves the property's access configuration for the permission level (as a bit flag) and checks if
     * the requested access level flag is set. This allows fine-grained control over which permission levels can read,
     * write, or otherwise interact with this property.
     *
     * @param permissionLevel
     *     The permission level to check (e.g., CREATOR, ADMIN, USER).
     * @param propertyAccessLevel
     *     The access level to verify (e.g., READ, WRITE).
     * @return True if the property grants the specified access level to the given permission level, false otherwise.
     *
     * @see #getAccessLevel(PermissionLevel)
     * @see PropertyAccessLevel#hasFlag(int, PropertyAccessLevel)
     */
    public boolean hasAccessLevel(PermissionLevel permissionLevel, PropertyAccessLevel propertyAccessLevel)
    {
        final int currentPropertyAccessLevel = getAccessLevel(permissionLevel);
        return PropertyAccessLevel.hasFlag(currentPropertyAccessLevel, propertyAccessLevel);
    }

    /**
     * Gets the property with the given serialization name.
     * <p>
     * Shortcut for {@link Registry#fromName(String)} with {@link #REGISTRY}.
     *
     * @param propertyKey
     *     The serialization name of the property.
     * @return The property with the given serialization name, or null if no property with that name exists.
     */
    public static @Nullable Property<?> fromName(String propertyKey)
    {
        return REGISTRY.fromName(propertyKey);
    }

    /**
     * Gets the property with the given
     *
     * @param propertyKey
     *     The key of the property.
     * @return The property with the given key, or null if no property with that key exists.
     */
    public static @Nullable Property<?> fromKey(NamespacedKey propertyKey)
    {
        return fromName(propertyKey.getFullKey());
    }

    /**
     * Casts the given value to the type of the property.
     * <p>
     * This method is a wrapper around {@link Class#cast(Object)} for {@link #getType()}.
     *
     * @param value
     *     The value to cast.
     * @return The value cast to the type of the property.
     *
     * @throws ClassCastException
     *     If the value cannot be cast to the type of the property.
     */
    @Contract("null -> null; !null -> !null")
    public @Nullable T cast(@Nullable Object value)
    {
        try
        {
            if (value == null)
                return null;
            return getType().cast(value);
        }
        catch (ClassCastException exception)
        {
            throw new IllegalArgumentException(
                String.format(
                    "Provided value '%s' is not of type '%s' for property '%s'.",
                    value,
                    getType(),
                    getFullKey()),
                exception
            );
        }
    }

    /**
     * Gets the access level for the given permission level.
     * <p>
     * Defaults to 0 if the command sender is not an owner of the structure.
     * <p>
     * The returned value is a bit flag whose values refer to the {@link PropertyAccessLevel} enum.
     *
     * @param permissionLevel
     *     The permission level to get the access level for.
     * @return The access level for the given permission level.
     */
    @CheckReturnValue
    public int getAccessLevel(PermissionLevel permissionLevel)
    {
        return switch (permissionLevel)
        {
            case CREATOR, ADMIN -> adminAccessLevel;
            case USER -> userAccessLevel;
            default -> 0;
        };
    }

    /**
     * Represents a builder for a property.
     * <p>
     * You can create a new property using
     *
     * @param <T>
     *     The type of the value of the property.
     */
    public static final class PropertyBuilder<T>
    {
        private final NamespacedKey namespacedKey;

        private final Class<T> type;

        private int userAccessLevel;

        private int adminAccessLevel;

        private List<PropertyScope> propertyScopes = List.of();

        private @Nullable T defaultValue = null;

        private PropertyBuilder(NamespacedKey namespacedKey, Class<T> type)
        {
            this.namespacedKey = namespacedKey;
            this.type = type;
        }

        /**
         * Builds the property.
         *
         * @return The newly built property.
         */
        public Property<T> build()
        {
            return new Property<>(
                namespacedKey,
                type,
                Util.requireNonNull(defaultValue, "Default Property value"),
                userAccessLevel,
                adminAccessLevel,
                propertyScopes
            );
        }

        /**
         * Sets up the access levels of both users and admins to ensure neither has any kind of access to this
         * property.
         * <p>
         * This is the default state, so calling this method does nothing unless anything else was configured
         * previously.
         *
         * @return This builder.
         */
        public PropertyBuilder<T> withoutUserOrAdminAccess()
        {
            this.userAccessLevel = PropertyAccessLevel.NONE.getFlag();
            this.adminAccessLevel = PropertyAccessLevel.NONE.getFlag();
            return this;
        }

        /**
         * Sets the property access level for users.
         *
         * @param userAccessLevel
         *     The level of access a user had over this property as a bitflag. See {@link PropertyAccessLevel}.
         * @return This builder.
         */
        public PropertyBuilder<T> withUserAccessLevels(int userAccessLevel)
        {
            this.userAccessLevel = userAccessLevel;
            return this;
        }

        /**
         * Sets the property access level for users.
         *
         * @param levels
         *     The level(s) of access a user should have over this property.
         * @return This builder.
         */
        public PropertyBuilder<T> withUserAccessLevels(PropertyAccessLevel... levels)
        {
            return withUserAccessLevels(PropertyAccessLevel.getFlagOf(levels));
        }

        /**
         * Sets the property access level for users.
         * <p>
         * This is a superset of the user access level by definition.
         * <p>
         * For example, if the user access level is 1 and the admin access level is 2, the resulting admin access level
         * is 1 + 2.
         *
         * @param adminAccessLevel
         *     The level of access an admin had over this property as a bitflag. See {@link PropertyAccessLevel}.
         * @return This builder.
         */
        public PropertyBuilder<T> withAdminAccessLevels(int adminAccessLevel)
        {
            this.adminAccessLevel = adminAccessLevel;
            return this;
        }

        /**
         * Sets the property access level for users.
         * <p>
         * This is a superset of the user access level by definition.
         * <p>
         * For example, if the user access level is 1 and the admin access level is 2, the resulting admin access level
         * is 1 + 2.
         *
         * @param levels
         *     The level(s) of access an admin should have over this property.
         * @return This builder.
         */
        public PropertyBuilder<T> withAdminAccessLevels(PropertyAccessLevel... levels)
        {
            return withAdminAccessLevels(PropertyAccessLevel.getFlagOf(levels));
        }

        /**
         * Sets the scopes in which this property is used.
         * <p>
         * This determines which parts of the structure are affected by this property.
         * <p>
         * For example, the {@link #BLOCKS_TO_MOVE} property is used in the animation scope, so changing the value of
         * this property will clear all cached values related to the animation, such as the animation range.
         * <p>
         * This defaults to no scopes.
         *
         * @param propertyScopes
         *     The scopes in which this property is used.
         * @return This builder.
         */
        public PropertyBuilder<T> withPropertyScopes(PropertyScope... propertyScopes)
        {
            this.propertyScopes = List.of(propertyScopes);
            return this;
        }

        /**
         * Sets the default value of the property.
         * <p>
         * When the property being created is a default property for a structure type, all instances of that structure
         * type will have this property set to the default value.
         * <p>
         * This defaults to {@code null}.
         *
         * @param defaultValue
         *     The default value of the property.
         * @return This builder.
         */
        public PropertyBuilder<T> withDefaultValue(@Nullable T defaultValue)
        {
            this.defaultValue = Util.requireNonNull(defaultValue, "Default Property value");
            return this;
        }
    }

    /**
     * The registry of all registered properties.
     * <p>
     * All instances of the {@link Property} class are registered in this class.
     */
    static final class Registry implements IDebuggable
    {
        /**
         * The map of all registered properties mapped by their {@link Property#getFullKey()} to the property instance.
         */
        private final Map<String, Property<?>> registeredProperties = new ConcurrentHashMap<>(10);

        /**
         * Gets the property with the given serialization name.
         *
         * @param propertyKey
         *     The serialization name of the property.
         * @return The property with the given serialization name, or null if no property with that name exists.
         */
        public @Nullable Property<?> fromName(String propertyKey)
        {
            return registeredProperties.get(propertyKey);
        }

        /**
         * Registers the given property.
         * <p>
         * If a property with the same name already exists, a warning will be logged and the old property will be
         * replaced.
         * <p>
         * This method is thread-safe and handles the REGISTERED_PROPERTIES map atomically.
         *
         * @param property
         *     The property to register.
         */
        private void register(Property<?> property)
        {
            registeredProperties.compute(
                property.getFullKey(), (key, value) ->
                {
                    if (value != null && !value.equals(property))
                    {
                        throw new IllegalArgumentException(String.format(
                            "Cannot register property '%s' because a property with that key already exists: %s",
                            property,
                            value
                        ));
                    }
                    return property;
                });
        }

        @Override
        public String getDebugInformation()
        {
            return "Registered properties: " + StringUtil.formatCollection(registeredProperties.keySet());
        }
    }
}
