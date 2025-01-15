package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IKeyed;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;
import nl.pim16aap2.animatedarchitecture.core.audio.AudioSet;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.util.LazyValue;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents a type of Structure.
 */
public abstract class StructureType implements IKeyed
{
    /**
     * The key that represents the name of this {@link StructureType} in its namespace.
     * <p>
     * The namespace is the name of the plugin that owns this {@link StructureType} and the key is the simple name of
     * the {@link StructureType}.
     *
     * @return The key that represents the name of this {@link StructureType} in its namespace.
     */
    @Getter
    protected final NamespacedKey namespacedKey;

    /**
     * Gets the version of this {@link StructureType}. Note that changing the version creates a whole new
     * {@link StructureType} and you'll have to take care of the transition.
     *
     * @return The version of this {@link StructureType}.
     */
    @Getter
    protected final int version;

    /**
     * Obtains the value of this type that represents the key in the translation system.
     *
     * @return The value of this type that represents the key in the translation system.
     */
    @Getter
    protected final String localizationKey;

    /**
     * The full name and the version as a single String.
     */
    @Getter
    private final String fullNameWithVersion;

    /**
     * Gets a set of all theoretically valid {@link MovementDirection} for this given type. It does NOT take the
     * physical aspects of the {@link Structure} into consideration. Therefore, the actual set of valid
     * {@link MovementDirection}s is most likely going to be a subset of those returned by this method.
     *
     * @return A set of all valid {@link MovementDirection} for this given type.
     */
    @Getter
    private final Set<MovementDirection> validMovementDirections;

    /**
     * Gets a list of all theoretically valid {@link MovementDirection} for this given type. It does NOT take the
     * physical aspects of the {@link Structure} into consideration. Therefore, the actual list of valid
     * {@link MovementDirection}s is most likely going to be a subset of those returned by this method.
     *
     * @return A list of all valid {@link MovementDirection} for this given type.
     */
    @Getter
    private final List<MovementDirection> validOpenDirectionsList;

    /**
     * The properties applicable to this type.
     *
     * @return The properties applicable to this type.
     */
    @Getter
    private final List<Property<?>> properties;

    private final LazyValue<StructureSerializer<?>> lazyStructureSerializer;

    /**
     * Constructs a new {@link StructureType}. Don't forget to also register it using
     * {@link StructureTypeManager#register(StructureType)}.
     *
     * @param namespacedKey
     *     The key that represents the name of this {@link StructureType} in its namespace.
     *     <p>
     *     The namespace is the name of the plugin that owns this {@link StructureType} and the key is the simple name
     *     of the {@link StructureType}.
     *     <p>
     *     E.g.: "animatedarchitecture:windmill" or "animatedarchitecture:bigdoor".
     * @param version
     *     The version of this {@link StructureType}.
     *     <p>
     *     This is used for serialization purposes. If you change the structure in a way that makes it incompatible with
     *     the previous version, you should increase this number. This will allow you to handle the transition between
     *     the old and new version. See {@link StructureSerializer} for more information.
     * @param validMovementDirections
     *     The valid movement directions for this structure.
     * @param supportedProperties
     *     The properties that are supported by this type.
     * @param localizationKey
     *     The key that is used to localize the name of this {@link StructureType}. For example,
     *     "structure.type.revolving_door".
     */
    protected StructureType(
        NamespacedKey namespacedKey,
        int version,
        List<MovementDirection> validMovementDirections,
        List<Property<?>> supportedProperties,
        String localizationKey)
    {
        this.namespacedKey = namespacedKey;

        this.version = version;
        this.validMovementDirections =
            validMovementDirections.isEmpty() ?
                EnumSet.noneOf(MovementDirection.class) :
                EnumSet.copyOf(validMovementDirections);
        this.validOpenDirectionsList = List.copyOf(this.validMovementDirections);
        this.properties = List.copyOf(supportedProperties);
        this.localizationKey = localizationKey;
        this.fullNameWithVersion = this.getFullKey() + ":" + version;

        lazyStructureSerializer = new LazyValue<>(() -> new StructureSerializer<>(this));
    }

    /**
     * Constructs a new {@link StructureType}. Don't forget to also register it using
     * {@link StructureTypeManager#register(StructureType)}.
     *
     * @param pluginName
     *     The name of the plugin that owns this {@link StructureType}.
     *     <p>
     *     The name can only contain (lowercase) letters, numbers, and underscores. Upper case letters will be converted
     *     to lowercase.
     * @param simpleName
     *     The simple name of this {@link StructureType}.
     *     <p>
     *     The name can only contain (lowercase) letters, numbers, and underscores. Upper case letters will be converted
     *     to lowercase.
     *     <p>
     *     The simple name is the name of the {@link StructureType} without the plugin name. For example, "windmill",
     *     "bigdoor", "flag", etc.
     * @param version
     *     The version of this {@link StructureType}.
     *     <p>
     *     This is used for serialization purposes. If you change the structure in a way that makes it incompatible with
     *     the previous version, you should increase this number. This will allow you to handle the transition between
     *     the old and new version. See {@link StructureSerializer} for more information.
     * @param validMovementDirections
     *     The valid movement directions for this structure.
     * @param supportedProperties
     *     The properties that are supported by this type.
     * @param localizationKey
     *     The key that is used to localize the name of this {@link StructureType}. For example,
     *     "structure.type.revolving_door".
     */
    protected StructureType(
        String pluginName,
        String simpleName,
        int version,
        List<MovementDirection> validMovementDirections,
        List<Property<?>> supportedProperties,
        String localizationKey)
    {
        this(
            new NamespacedKey(pluginName, simpleName),
            version,
            validMovementDirections,
            supportedProperties,
            localizationKey
        );
    }

    /**
     * Gets the simple name of the {@link StructureType}.
     * <p>
     * The simple name is the name of the {@link StructureType} without the plugin name. For example, "windmill",
     * "bigdoor", "flag", etc.
     *
     * @return The simple name of the {@link StructureType}.
     */
    public final String getSimpleName()
    {
        return getNamespacedKey().getKey();
    }

    /**
     * Gets the full name of the {@link StructureType}.
     * <p>
     * This is the fully qualified name of the {@link StructureType} and includes the plugin name. For example,
     * "animatedarchitecture:windmill", "animatedarchitecture:bigdoor", "animatedarchitecture:flag", etc.
     * <p>
     * This is a shortcut for {@code getNamespacedKey().getFullKey()}.
     *
     * @return The full name of the {@link StructureType}.
     */
    @Override
    public final String getFullKey()
    {
        return getNamespacedKey().getFullKey();
    }

    /**
     * Gets the {@link StructureSerializer} for this type.
     *
     * @return The {@link StructureSerializer}.
     */
    public final StructureSerializer<?> getStructureSerializer()
    {
        return lazyStructureSerializer.get();
    }

    /**
     * Checks if a given {@link MovementDirection} is valid for this type.
     *
     * @param movementDirection
     *     The {@link MovementDirection} to check.
     * @return True if the provided {@link MovementDirection} is valid for this type, otherwise false.
     */
    public final boolean isValidOpenDirection(MovementDirection movementDirection)
    {
        return validMovementDirections.contains(movementDirection);
    }

    /**
     * Gets the main structure class of the type.
     *
     * @return THe class of the structure.
     */
    public abstract Class<? extends Structure> getStructureClass();

    /**
     * Creates (and registers) a new {@link Creator} for this type.
     *
     * @param context
     *     The context to run the creator in.
     * @param player
     *     The player who will own the {@link Creator}.
     * @return The newly created {@link Creator}.
     */
    public Creator getCreator(ToolUser.Context context, IPlayer player)
    {
        return getCreator(context, player, null);
    }

    /**
     * Creates (and registers) a new {@link Creator} for this type.
     *
     * @param context
     *     The context to run the creator in.
     * @param player
     *     The player who will own the {@link Creator}.
     * @param name
     *     The name that will be given to the structure.
     * @return The newly created {@link Creator}.
     */
    public abstract Creator getCreator(ToolUser.Context context, IPlayer player, @Nullable String name);

    /**
     * Gets the {@link AudioSet} for this type.
     *
     * @return The {@link AudioSet} for this type.
     */
    public @Nullable AudioSet getAudioSet()
    {
        return null;
    }

    @Override
    public final String toString()
    {
        return "StructureType[@" + Integer.toHexString(hashCode()) + "] " + getFullNameWithVersion();
    }

    @Override
    public final int hashCode()
    {
        // There may only ever exist 1 instance of each StructureType.
        return super.hashCode();
    }

    @Override
    public final boolean equals(@Nullable Object obj)
    {
        // There may only ever exist 1 instance of each StructureType.
        return super.equals(obj);
    }

    /**
     * Gets the permission that is required to create a structure of this type.
     *
     * @return The permission that is required to create a structure of this type.
     */
    public String getCreationPermission()
    {
        return Constants.PERMISSION_PREFIX_USER + "create." + getSimpleName();
    }
}
