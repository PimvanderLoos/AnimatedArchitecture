package nl.pim16aap2.bigdoors.doortypes;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorSerializer;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * This class represents a type of Door. "Door" in this case, refers to any kind of animated object, so not necessarily
 * a door.
 *
 * @author Pim
 */
public abstract class DoorType
{
    /**
     * Gets the name of the plugin that owns this {@link DoorType}.
     *
     * @return The name of the plugin that owns this {@link DoorType}.
     */
    @Getter
    protected final String pluginName;

    /**
     * Gets the name of this {@link DoorType}. Note that this is always in lower case!
     *
     * @return The name of this {@link DoorType}.
     */
    @Getter
    protected final String simpleName;

    /**
     * Gets the version of this {@link DoorType}. Note that changing the version creates a whole new {@link DoorType}
     * and you'll have to take care of the transition.
     *
     * @return The version of this {@link DoorType}.
     */
    @Getter
    protected final int typeVersion;

    /**
     * Obtains the value of this type that represents the key in the translation system.
     *
     * @return The value of this type that represents the key in the translation system.
     */
    @Getter
    protected final String localizationKey;

    /**
     * The fully-qualified name of this {@link DoorType}.
     */
    @Getter
    private final String fullName;

    /**
     * Gets a list of all theoretically valid {@link RotateDirection} for this given type. It does NOT take the physical
     * aspects of the {@link DoorBase} into consideration. Therefore, the actual list of valid {@link RotateDirection}s
     * is most likely going to be a subset of those returned by this method.
     *
     * @return A list of all valid {@link RotateDirection} for this given type.
     */
    @Getter
    private final List<RotateDirection> validOpenDirections;

    private volatile @Nullable DoorSerializer<?> doorSerializer;

    /**
     * Constructs a new {@link DoorType}. Don't forget to also register it using {@link
     * nl.pim16aap2.bigdoors.managers.DoorTypeManager#registerDoorType(DoorType)}.
     *
     * @param pluginName
     *     The name of the plugin that owns this {@link DoorType}.
     * @param simpleName
     *     The 'simple' name of this {@link DoorType}. E.g. "Flag", or "Windmill".
     * @param typeVersion
     *     The version of this {@link DoorType}. Note that changing the version results in a completely new {@link
     *     DoorType}, as far as the database is concerned. This fact can be used if the parameters of the constructor
     *     for this type need to be changed.
     */
    protected DoorType(String pluginName, String simpleName, int typeVersion, List<RotateDirection> validOpenDirections,
                       String localizationKey)
    {
        this.pluginName = pluginName;
        this.simpleName = simpleName.toLowerCase(Locale.ENGLISH);
        this.typeVersion = typeVersion;
        this.validOpenDirections = validOpenDirections;
        this.localizationKey = localizationKey;
        fullName = String.format("%s_%s_%d", getPluginName(), getSimpleName(), getTypeVersion())
                         .toLowerCase(Locale.ENGLISH);
    }

    /**
     * Gets the {@link DoorSerializer} for this type.
     *
     * @return The {@link DoorSerializer}.
     */
    @SuppressWarnings("ConstantConditions")
    public DoorSerializer<?> getDoorSerializer()
    {
        if (doorSerializer != null)
            return doorSerializer;
        synchronized (this)
        {
            if (doorSerializer == null)
                doorSerializer = new DoorSerializer<>(getDoorClass());
            return doorSerializer;
        }
    }

    /**
     * Checks if a given {@link RotateDirection} is valid for this type.
     *
     * @param rotateDirection
     *     The {@link RotateDirection} to check.
     * @return True if the provided {@link RotateDirection} is valid for this type, otherwise false.
     */
    public final boolean isValidOpenDirection(RotateDirection rotateDirection)
    {
        return validOpenDirections.contains(rotateDirection);
    }

    /**
     * Gets the main door class of the type.
     *
     * @return THe class of the door.
     */
    public abstract Class<? extends AbstractDoor> getDoorClass();

    /**
     * Creates (and registers) a new {@link Creator} for this type.
     *
     * @param context
     *     The context to run the creator in.
     * @param player
     *     The player who will own the {@link Creator}.
     * @return The newly created {@link Creator}.
     */
    public Creator getCreator(Creator.Context context, IPPlayer player)
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
     *     The name that will be given to the door.
     * @return The newly created {@link Creator}.
     */
    public abstract Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name);

    @Override
    public final String toString()
    {
        return getPluginName() + ":" + getSimpleName() + ":" + getTypeVersion();
    }

    @Override
    public final int hashCode()
    {
        // There may only ever exist 1 instance of each DoorType.
        return super.hashCode();
    }

    @Override
    public final boolean equals(@Nullable Object obj)
    {
        // There may only ever exist 1 instance of each DoorType.
        return super.equals(obj);
    }
}
