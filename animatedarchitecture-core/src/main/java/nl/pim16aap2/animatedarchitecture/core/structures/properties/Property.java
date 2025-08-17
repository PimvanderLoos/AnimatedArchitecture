package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IKeyed;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.structures.RedstoneMode;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final PropertyAccessLevel propertyAccessLevel;

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
     * The scopes in which this property is used.
     * <p>
     * This is used to prevent side effects of changing the property value. For example, clearing cached values related
     * to the property, such as the animation range when changing the 'blocks to move' property.
     */
    @Getter
    private final List<PropertyScope> propertyScopes;

    private final boolean canBeAddedByUser;

    /**
     * A property for structures whose animation speed is variable.
     */
    public static final Property<Double> ANIMATION_SPEED_MULTIPLIER =
        builder("ANIMATION_SPEED_MULTIPLIER", Double.class)
            .canBeAddedByUser()
            .withDefaultValue(1.0D)
            .isHidden() // I am not sure if the animator currently uses this property.
            .build();

    /**
     * A property for structures that move a certain amount of blocks when activated.
     */
    public static final Property<Integer> BLOCKS_TO_MOVE =
        builder("BLOCKS_TO_MOVE", Integer.class)
            .isEditable()
            .withDefaultValue(0)
            // Changing the blocks to move may affect things like animation range.
            .withPropertyScopes(PropertyScope.ANIMATION)
            .build();

    /**
     * A property for structures that have a defined open and closed state.
     */
    public static final Property<Boolean> OPEN_STATUS =
        builder("OPEN_STATUS", Boolean.class)
            .isEditable()
            .withDefaultValue(false)
            .withPropertyScopes(PropertyScope.ANIMATION, PropertyScope.REDSTONE)
            .build();

    /**
     * A property for structures that can rotate multiples of 90 degrees.
     */
    public static final Property<Integer> QUARTER_CIRCLES =
        builder("QUARTER_CIRCLES", Integer.class)
            .isEditable()
            .withDefaultValue(1)
            .isHidden() // Quarter circles are not yet fully supported.
            // Changing the quarter circles may affect things like animation range.
            .withPropertyScopes(PropertyScope.ANIMATION)
            .build();

    /**
     * A property for structures that can have different redstone modes.
     */
    public static final Property<RedstoneMode> REDSTONE_MODE =
        builder("REDSTONE_MODE", RedstoneMode.class)
            .isReadOnly()
            .withDefaultValue(RedstoneMode.DEFAULT)
            // Changing the redstone mode may affect the current redstone action.
            .withPropertyScopes(PropertyScope.REDSTONE)
            .build();

    /**
     * A property for structures that have a defined rotation point.
     */
    public static final Property<Vector3Di> ROTATION_POINT =
        builder("ROTATION_POINT", Vector3Di.class)
            .isEditable()
            .withDefaultValue(new Vector3Di(0, 0, 0))
            // Changing the rotation point may affect things like animation range.
            .withPropertyScopes(PropertyScope.ANIMATION)
            .build();

    @VisibleForTesting
    Property(
        NamespacedKey namespacedKey,
        Class<T> type,
        T defaultValue,
        PropertyAccessLevel propertyAccessLevel,
        List<PropertyScope> scopes,
        boolean canBeAddedByUser)
    {
        this.namespacedKey = Util.requireNonNull(namespacedKey, "NamespacedKey");
        this.type = Util.requireNonNull(type, "Property type");
        this.defaultValue = Util.requireNonNull(defaultValue, "Default Property value");
        this.propertyAccessLevel = Util.requireNonNull(propertyAccessLevel, "Property access level");
        this.propertyScopes = List.copyOf(scopes);
        this.canBeAddedByUser = canBeAddedByUser;

        if (!type.isInstance(defaultValue))
            throw new IllegalArgumentException("Default value " + defaultValue + " is not of type " + type.getName());

        REGISTRY.register(this);
    }

    /**
     * Determines whether this property can be added to a structure if it is not defined in the list of default
     * properties of the structure type.
     * <p>
     * When false, attempting to add this property to a structure via a command will result in an exception. It can only
     * be part of a structure if it is defined in the list of default properties of the structure type (see
     * {@link StructureType#getProperties()}) or by adding it programmatically.
     *
     * @return {@code true} if the property can be added by the user, {@code false} otherwise.
     */
    public boolean canBeAddedByUser()
    {
        return canBeAddedByUser;
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
     * Gets the property with the given name (see {@link Property#getFullKey()}).
     *
     * @param propertyName
     *     The name of the property.
     * @return The property with the given name, or null if no property with that name exists.
     */
    public static @Nullable Property<?> fromName(String propertyName)
    {
        return REGISTRY.fromName(propertyName);
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

    public static IDebuggable getDebuggableRegistry()
    {
        return REGISTRY;
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

        private PropertyAccessLevel propertyAccessLevel = PropertyAccessLevel.READ_ONLY;

        private List<PropertyScope> propertyScopes = List.of();

        private @Nullable T defaultValue = null;

        private boolean canBeAddedByUser = false;

        private PropertyBuilder(NamespacedKey namespacedKey, Class<T> type)
        {
            this.namespacedKey = namespacedKey;
            this.type = type;
        }

        /**
         * Builds the property.
         *
         * @return The property.
         */
        public Property<T> build()
        {
            return new Property<>(
                namespacedKey,
                type,
                Util.requireNonNull(defaultValue, "Default Property value"),
                propertyAccessLevel,
                propertyScopes,
                canBeAddedByUser
            );
        }

        /**
         * Sets the property access level.
         * <p>
         * This determines to what extent a user can interact with the property.
         * <p>
         * This defaults to {@link PropertyAccessLevel#READ_ONLY}.
         *
         * @param propertyAccessLevel
         *     The property access level.
         * @return The builder.
         */
        public PropertyBuilder<T> withPropertyAccessLevel(PropertyAccessLevel propertyAccessLevel)
        {
            this.propertyAccessLevel = propertyAccessLevel;
            return this;
        }

        /**
         * Sets the property access level to {@link PropertyAccessLevel#USER_EDITABLE}.
         * <p>
         * See {@link #withPropertyAccessLevel(PropertyAccessLevel)} for more information.
         *
         * @return The builder.
         */
        public PropertyBuilder<T> isHidden()
        {
            return withPropertyAccessLevel(PropertyAccessLevel.HIDDEN);
        }

        /**
         * Sets the property access level to {@link PropertyAccessLevel#READ_ONLY}.
         * <p>
         * See {@link #withPropertyAccessLevel(PropertyAccessLevel)} for more information.
         *
         * @return The builder.
         */
        public PropertyBuilder<T> isReadOnly()
        {
            return withPropertyAccessLevel(PropertyAccessLevel.READ_ONLY);
        }

        /**
         * Sets the property access level to {@link PropertyAccessLevel#USER_EDITABLE}.
         * <p>
         * See {@link #withPropertyAccessLevel(PropertyAccessLevel)} for more information.
         *
         * @return The builder.
         */
        public PropertyBuilder<T> isEditable()
        {
            return withPropertyAccessLevel(PropertyAccessLevel.USER_EDITABLE);
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
         * @return The builder.
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
         * @return The builder.
         */
        public PropertyBuilder<T> withDefaultValue(T defaultValue)
        {
            this.defaultValue = Objects.requireNonNull(defaultValue);
            return this;
        }

        /**
         * Sets whether this property can be added to a structure if it is not defined in the list of default properties
         * of the structure type.
         * <p>
         * When false, attempting to add this property to a structure via a command will result in an exception. It can
         * only be part of a structure if it is defined in the list of default properties of the structure type (see
         * {@link StructureType#getProperties()}) or by adding it programmatically.
         * <p>
         * This defaults to {@code false}.
         *
         * @return The builder.
         */
        public PropertyBuilder<T> canBeAddedByUser()
        {
            return canBeAddedByUser(true);
        }

        /**
         * See {@link #canBeAddedByUser()} for more information.
         *
         * @param canBeAddedByUser
         *     True if the property can be added by the user, false otherwise.
         * @return The builder.
         */
        public PropertyBuilder<T> canBeAddedByUser(boolean canBeAddedByUser)
        {
            this.canBeAddedByUser = canBeAddedByUser;
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
