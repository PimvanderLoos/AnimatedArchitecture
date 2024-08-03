package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IKeyed;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.structures.RedstoneMode;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.Collections;
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
     * A set of all registered property names.
     */
    private static final Map<String, Property<?>> REGISTERED_PROPERTIES = new ConcurrentHashMap<>(10);

    /**
     * An unmodifiable view of the registered properties.
     */
    private static final Map<String, Property<?>> UNMODIFIABLE_REGISTERED_PROPERTIES =
        Collections.unmodifiableMap(REGISTERED_PROPERTIES);

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
        null
    );

    /**
     * A property for structures that move a certain amount of blocks when activated.
     */
    public static final Property<Integer> BLOCKS_TO_MOVE = new Property<>(
        "BLOCKS_TO_MOVE",
        Integer.class,
        null,
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
        // The open status affects things like animation direction.
        PropertyScope.ANIMATION,
        // Changing the open status may affect the current redstone action.
        PropertyScope.REDSTONE
    );

    /**
     * A property for structures that can have different redstone modes.
     */
    public static final Property<RedstoneMode> REDSTONE_MODE = new Property<>(
        "REDSTONE_MODE",
        RedstoneMode.class,
        RedstoneMode.DEFAULT,
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
        // Changing the rotation point may affect things like animation range.
        PropertyScope.ANIMATION
    );

    private Property(NamespacedKey namespacedKey, Class<T> type, @Nullable T defaultValue, PropertyScope... scopes)
    {
        this.namespacedKey = namespacedKey;
        this.type = type;
        this.defaultValue = defaultValue;
        this.propertyScopes = List.of(scopes);

        if (defaultValue != null && !type.isInstance(defaultValue))
            throw new IllegalArgumentException("Default value " + defaultValue + " is not of type " + type.getName());

        registerProperty(this);
    }

    /**
     * Creates a new property in the default namespace with the given name, type, and default value.
     *
     * @param name
     *     The name of the property.
     * @param type
     *     The type of the property.
     * @param defaultValue
     *     The default value of the property.
     */
    private Property(String name, Class<T> type, @Nullable T defaultValue, PropertyScope... scopes)
    {
        this(new NamespacedKey(Constants.PLUGIN_NAME, name), type, defaultValue, scopes);
    }

    /**
     * Creates a new property with the given owner, name, type, and default value.
     *
     * @param owner
     *     The name of the plugin that owns this property. This is used to prevent conflicts between properties between
     *     different plugins.
     *     <p>
     *     All properties supplied by this plugin are set to {@link Constants#PLUGIN_NAME}.
     * @param name
     *     The name of the property that is used for serialization.
     * @param type
     *     The type of the property.
     * @param defaultValue
     *     The default value of the property. If the property owner is the same as the plugin name. See
     *     {@link Constants#PLUGIN_NAME}.
     *     <p>
     *     This is to prevent conflicts between properties between different plugins.
     */
    public Property(String owner, String name, Class<T> type, @Nullable T defaultValue, PropertyScope... scopes)
    {
        this(serializationName(owner, name), type, defaultValue, scopes);
    }

    /**
     * Registers the given property.
     * <p>
     * If a property with the same name already exists, a warning will be logged and the old property will be replaced.
     * <p>
     * This method is thread-safe and handles the REGISTERED_PROPERTIES map atomically.
     *
     * @param property
     *     The property to register.
     */
    private static void registerProperty(Property<?> property)
    {
        REGISTERED_PROPERTIES.compute(property.getFullKey(), (key, value) ->
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

    /**
     * Gets a map of all registered properties.
     * <p>
     * The keys are the full keys of the properties.
     * <p>
     * The values are the properties themselves.
     * <p>
     * Note that the map returns an unmodifiable view of the registered properties. However, the underlying map may
     * still be modified when new properties are registered.
     *
     * @return An unmodifiable map of all registered properties.
     */
    public static Map<String, Property<?>> getRegisteredProperties()
    {
        return UNMODIFIABLE_REGISTERED_PROPERTIES;
    }

    /**
     * Gets the property with the given serialization name.
     *
     * @param propertyKey
     *     The serialization name of the property.
     * @return The property with the given serialization name, or null if no property with that name exists.
     */
    public static @Nullable Property<?> fromName(String propertyKey)
    {
        return REGISTERED_PROPERTIES.get(propertyKey);
    }

    /**
     * Creates a new {@link NamespacedKey} with the given owner and name.
     *
     * @param owner
     *     The name of the plugin that owns this property. This is used to prevent conflicts between properties between
     *     different plugins.
     *     <p>
     *     All properties supplied by this plugin are set to {@link Constants#PLUGIN_NAME}.
     * @param name
     *     The name of the property that is used for serialization.
     * @throws IllegalArgumentException
     *     If the owner is the same as the plugin name.
     */
    private static NamespacedKey serializationName(String owner, String name)
    {
        if (Constants.PLUGIN_NAME.equals(owner))
            throw new IllegalArgumentException("Owner cannot be the same as the plugin name.");
        return new NamespacedKey(owner, name);
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
    public T cast(@Nullable Object value)
    {
        return type.cast(value);
    }
}
