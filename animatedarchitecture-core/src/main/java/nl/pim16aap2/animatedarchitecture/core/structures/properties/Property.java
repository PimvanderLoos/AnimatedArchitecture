package nl.pim16aap2.animatedarchitecture.core.structures.properties;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IKeyed;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nullable;
import java.util.Set;
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
@Getter
@ToString
@EqualsAndHashCode
public final class Property<T> implements IKeyed
{
    /**
     * A set of all registered property names.
     */
    private static final Set<String> REGISTERED_NAMES = ConcurrentHashMap.newKeySet();

    /**
     * The name of the property that is used for serialization.
     * <p>
     * Note that this name should be unique for each property.
     *
     * @return The name of the property.
     */
    private final NamespacedKey namespacedKey;

    /**
     * The type of the property.
     *
     * @return The type of the property.
     */
    private final Class<T> type;

    /**
     * The default value of the property.
     *
     * @return The default value of the property.
     */
    private final @Nullable T defaultValue;

    /**
     * A property for structures that have a defined open and closed state.
     */
    public static final Property<Boolean> OPEN_STATUS = new Property<>("OPEN_STATUS", Boolean.class, false);

    private Property(NamespacedKey namespacedKey, Class<T> type, @Nullable T defaultValue)
    {
        this.namespacedKey = namespacedKey;
        this.type = type;
        this.defaultValue = defaultValue;

        if (!REGISTERED_NAMES.add(namespacedKey.getKey()))
            throw new IllegalArgumentException("Property with name " + namespacedKey.getKey() + " already exists.");
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
    private Property(String name, Class<T> type, @Nullable T defaultValue)
    {
        this(new NamespacedKey(Constants.PLUGIN_NAME, name), type, defaultValue);
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
    public Property(String owner, String name, Class<T> type, @Nullable T defaultValue)
    {
        this(serializationName(owner, name), type, defaultValue);
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
        if (value == null)
            return null;
        if (type.isInstance(value))
            return type.cast(value);
        throw new ClassCastException("Cannot cast " + value + " to " + type.getName());
    }
}
