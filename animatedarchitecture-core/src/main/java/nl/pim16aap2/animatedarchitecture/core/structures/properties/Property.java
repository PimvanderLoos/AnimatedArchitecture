package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IKeyed;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.structures.RedstoneMode;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

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
 *     The type of the property.
 */
@ToString
@EqualsAndHashCode
@Flogger
public final class Property<T> implements IKeyed
{
    /**
     * The registry of all registered properties.
     */
    public static final Registry REGISTRY = new Registry();

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
    private final @Nullable T defaultValue;

    /**
     * The scopes in which this property is used.
     */
    @Getter
    private final List<PropertyScope> propertyScopes;

    /**
     * A property for structures whose animation speed is variable.
     */
    public static final Property<Double> ANIMATION_SPEED_MULTIPLIER = new Property<>(
        "ANIMATION_SPEED_MULTIPLIER",
        Double.class,
        null,
        PropertyAccessLevel.HIDDEN // I am not sure if the animator currently uses this property.
    );

    /**
     * A property for structures that move a certain amount of blocks when activated.
     */
    public static final Property<Integer> BLOCKS_TO_MOVE = new Property<>(
        "BLOCKS_TO_MOVE",
        Integer.class,
        null,
        PropertyAccessLevel.USER_EDITABLE,
        // Changing the blocks to move may affect things like animation range.
        PropertyScope.ANIMATION
    );

    /**
     * A property for structures that have a defined open and closed state.
     */
    public static final Property<Boolean> OPEN_STATUS = new Property<>(
        "OPEN_STATUS",
        Boolean.class,
        false,
        PropertyAccessLevel.USER_EDITABLE,
        // The open status affects things like animation direction.
        PropertyScope.ANIMATION,
        // Changing the open status may affect the current redstone action.
        PropertyScope.REDSTONE
    );

    /**
     * A property for structures that can rotate multiples of 90 degrees.
     */
    public static final Property<Integer> QUARTER_CIRCLES = new Property<>(
        "QUARTER_CIRCLES",
        Integer.class,
        1,
        PropertyAccessLevel.HIDDEN, // Quarter circles are not yet fully supported.
        // Changing how many quarter circles the structure rotates affects things like animation range.
        PropertyScope.ANIMATION
    );

    /**
     * A property for structures that can have different redstone modes.
     */
    public static final Property<RedstoneMode> REDSTONE_MODE = new Property<>(
        "REDSTONE_MODE",
        RedstoneMode.class,
        RedstoneMode.DEFAULT,
        PropertyAccessLevel.HIDDEN, // There are currently no implementations for the alternative redstone modes.
        // Changing the redstone mode may affect the current redstone action.
        PropertyScope.REDSTONE
    );

    /**
     * A property for structures that have a defined rotation point.
     */
    public static final Property<Vector3Di> ROTATION_POINT = new Property<>(
        "ROTATION_POINT",
        Vector3Di.class,
        null,
        PropertyAccessLevel.USER_EDITABLE,
        // Changing the rotation point may affect things like animation range.
        PropertyScope.ANIMATION
    );

    /**
     * Creates a new property.
     *
     * @param namespacedKey
     *     The namespace and key of the property.
     * @param type
     *     The type of the property.
     * @param defaultValue
     *     The default value of the property.
     *     <p>
     *     This is the value that will be used if a structure whose type has this property does not have a value set for
     *     this property.
     * @param propertyAccessLevel
     *     The level of access users have to this property.
     * @param scopes
     *     The scopes in which this property is used.
     *     <p>
     *     This is used to prevent side effects of changing the property value. For example, clearing cached values
     *     related to the property, such as the animation range when changing the blocks to move.
     */
    public Property(
        NamespacedKey namespacedKey,
        Class<T> type,
        @Nullable T defaultValue,
        PropertyAccessLevel propertyAccessLevel,
        PropertyScope... scopes)
    {
        this.namespacedKey = namespacedKey;
        this.type = type;
        this.propertyAccessLevel = propertyAccessLevel;
        this.defaultValue = defaultValue;
        this.propertyScopes = List.of(scopes);

        if (defaultValue != null && !type.isInstance(defaultValue))
            throw new IllegalArgumentException("Default value " + defaultValue + " is not of type " + type.getName());

        REGISTRY.register(this);
    }

    /**
     * Creates a new property.
     *
     * @param name
     *     The name of the property.
     * @param type
     *     The type of the property.
     * @param defaultValue
     *     The default value of the property.
     * @param propertyAccessLevel
     *     The level of access users have to this property.
     * @param scopes
     *     The scopes in which this property is used.
     *     <p>
     *     This is used to prevent side effects of changing the property value. For example, clearing cached values
     *     related to the property, such as the animation range when changing the blocks to move.
     */
    private Property(
        String name,
        Class<T> type,
        @Nullable T defaultValue,
        PropertyAccessLevel propertyAccessLevel,
        PropertyScope... scopes)
    {
        this(new NamespacedKey(Constants.PLUGIN_NAME, name), type, defaultValue, propertyAccessLevel, scopes);
    }

    /**
     * Creates a new property.
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
     * @param defaultValue
     *     The default value of the property.
     *     <p>
     *     This is the value that will be used if a structure whose type has this property does not have a value set for
     *     this property.
     * @param propertyAccessLevel
     *     The level of access users have to this property.
     * @param scopes
     *     The scopes in which this property is used.
     *     <p>
     *     This is used to prevent side effects of changing the property value. For example, clearing cached values
     *     related to the property, such as the animation range when changing the blocks to move.
     */
    public Property(
        String owner,
        String name,
        Class<T> type,
        @Nullable T defaultValue,
        PropertyAccessLevel propertyAccessLevel,
        PropertyScope... scopes)
    {
        this(new NamespacedKey(owner, name), type, defaultValue, propertyAccessLevel, scopes);
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
    public @Nullable T cast(@Nullable Object value)
    {
        try
        {
            if (value == null)
                return null;
            return getType().cast(value);
        }
        catch (ClassCastException e)
        {
            throw new IllegalArgumentException("Provided incompatible value for property " + this, e);
        }
    }

    /**
     * The registry of all registered properties.
     * <p>
     * All instances of the {@link Property} class are registered in this class.
     */
    public static final class Registry implements IDebuggable
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
            registeredProperties.compute(property.getFullKey(), (key, value) ->
            {
                if (value != null)
                    log.atSevere().log(
                        "Property with name '%s' has already been registered with value '%s'!" +
                            " It will be replaced by property '%s'!",
                        key,
                        value,
                        property
                    );
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
