package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.audio.AudioSet;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureTypeManager;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import nl.pim16aap2.animatedarchitecture.core.util.LazyValue;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * This class represents a type of Structure.
 *
 * @author Pim
 */
public abstract class StructureType
{
    /**
     * Gets the name of the plugin that owns this {@link StructureType}.
     *
     * @return The name of the plugin that owns this {@link StructureType}.
     */
    @Getter
    protected final String pluginName;

    /**
     * Gets the name of this {@link StructureType}. Note that this is always in lower case!
     *
     * @return The name of this {@link StructureType}.
     */
    @Getter
    protected final String simpleName;

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
     * The fully-qualified name of this {@link StructureType} formatted as "pluginName_simpleName".
     */
    @Getter
    private final String fullName;

    /**
     * The {@link #fullName} and the version as a single String.
     */
    @Getter
    private final String fullNameWithVersion;

    /**
     * Gets a list of all theoretically valid {@link MovementDirection} for this given type. It does NOT take the
     * physical aspects of the {@link AbstractStructure} into consideration. Therefore, the actual list of valid
     * {@link MovementDirection}s is most likely going to be a subset of those returned by this method.
     *
     * @return A list of all valid {@link MovementDirection} for this given type.
     */
    @Getter
    private final Set<MovementDirection> validOpenDirections;

    private final LazyValue<StructureSerializer<?>> lazyStructureSerializer;

    /**
     * Constructs a new {@link StructureType}. Don't forget to also register it using
     * {@link StructureTypeManager#register(StructureType)}.
     *
     * @param pluginName
     *     The name of the plugin that owns this {@link StructureType}.
     * @param simpleName
     *     The 'simple' name of this {@link StructureType}. E.g. "Flag", or "Windmill".
     * @param version
     *     The version of this {@link StructureType}.
     */
    protected StructureType(
        String pluginName, String simpleName, int version, List<MovementDirection> validOpenDirections,
        String localizationKey)
    {
        this.pluginName = pluginName;
        this.simpleName = simpleName.toLowerCase(Locale.ENGLISH);
        this.version = version;
        this.validOpenDirections =
            validOpenDirections.isEmpty() ? EnumSet.noneOf(MovementDirection.class) :
            EnumSet.copyOf(validOpenDirections);
        this.localizationKey = localizationKey;
        this.fullName = formatFullName(getPluginName(), getSimpleName());
        this.fullNameWithVersion = fullName + ":" + version;

        lazyStructureSerializer = new LazyValue<>(() -> new StructureSerializer<>(this));
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
        return validOpenDirections.contains(movementDirection);
    }

    /**
     * Gets the main structure class of the type.
     *
     * @return THe class of the structure.
     */
    public abstract Class<? extends AbstractStructure> getStructureClass();

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

    public @Nullable AudioSet getAudioSet()
    {
        return null;
    }

    /**
     * Formats the given pluginName and simpleName into a fully-qualified name.
     *
     * @param pluginName
     *     The name of the plugin that owns this {@link StructureType}.
     * @param simpleName
     *     The 'simple' name of this {@link StructureType}. E.g. "Flag", or "Windmill".
     * @return The fully-qualified name of this {@link StructureType} formatted as
     */
    public static String formatFullName(String pluginName, String simpleName)
    {
        return String.format("%s:%s", pluginName, simpleName).toLowerCase(Locale.ENGLISH);
    }

    @Override
    public final String toString()
    {
        return "StructureType[@" + Integer.toHexString(hashCode()) + "] " +
            getPluginName() + ":" + getSimpleName() + ":" + getVersion();
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
}
