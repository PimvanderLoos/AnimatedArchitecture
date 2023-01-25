package nl.pim16aap2.bigdoors.movabletypes;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.audio.AudioSet;
import nl.pim16aap2.bigdoors.managers.MovableTypeManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableSerializer;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * This class represents a type of Movable.
 *
 * @author Pim
 */
public abstract class MovableType
{
    /**
     * Gets the name of the plugin that owns this {@link MovableType}.
     *
     * @return The name of the plugin that owns this {@link MovableType}.
     */
    @Getter
    protected final String pluginName;

    /**
     * Gets the name of this {@link MovableType}. Note that this is always in lower case!
     *
     * @return The name of this {@link MovableType}.
     */
    @Getter
    protected final String simpleName;

    /**
     * Gets the version of this {@link MovableType}. Note that changing the version creates a whole new
     * {@link MovableType} and you'll have to take care of the transition.
     *
     * @return The version of this {@link MovableType}.
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
     * The fully-qualified name of this {@link MovableType}.
     */
    @Getter
    private final String fullName;

    /**
     * Gets a list of all theoretically valid {@link MovementDirection} for this given type. It does NOT take the
     * physical aspects of the {@link AbstractMovable} into consideration. Therefore, the actual list of valid
     * {@link MovementDirection}s is most likely going to be a subset of those returned by this method.
     *
     * @return A list of all valid {@link MovementDirection} for this given type.
     */
    @Getter
    private final Set<MovementDirection> validOpenDirections;

    private volatile @Nullable MovableSerializer<?> movableSerializer;

    /**
     * Constructs a new {@link MovableType}. Don't forget to also register it using
     * {@link MovableTypeManager#registerMovableType(MovableType)}.
     *
     * @param pluginName
     *     The name of the plugin that owns this {@link MovableType}.
     * @param simpleName
     *     The 'simple' name of this {@link MovableType}. E.g. "Flag", or "Windmill".
     * @param typeVersion
     *     The version of this {@link MovableType}. Note that changing the version results in a completely new
     *     {@link MovableType}, as far as the database is concerned. This fact can be used if the parameters of the
     *     constructor for this type need to be changed.
     */
    protected MovableType(
        String pluginName, String simpleName, int typeVersion, List<MovementDirection> validOpenDirections,
        String localizationKey)
    {
        this.pluginName = pluginName;
        this.simpleName = simpleName.toLowerCase(Locale.ENGLISH);
        this.typeVersion = typeVersion;
        this.validOpenDirections =
            validOpenDirections.isEmpty() ? EnumSet.noneOf(MovementDirection.class) :
            EnumSet.copyOf(validOpenDirections);
        this.localizationKey = localizationKey;
        fullName = String.format("%s_%s_%d", getPluginName(), getSimpleName(), getTypeVersion())
                         .toLowerCase(Locale.ENGLISH);
    }

    /**
     * Gets the {@link MovableSerializer} for this type.
     *
     * @return The {@link MovableSerializer}.
     */
    @SuppressWarnings("ConstantConditions")
    public final MovableSerializer<?> getMovableSerializer()
    {
        if (movableSerializer != null)
            return movableSerializer;
        synchronized (this)
        {
            if (movableSerializer == null)
                movableSerializer = new MovableSerializer<>(getMovableClass());
            return movableSerializer;
        }
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
     * Gets the main movable class of the type.
     *
     * @return THe class of the movable.
     */
    public abstract Class<? extends AbstractMovable> getMovableClass();

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
     *     The name that will be given to the movable.
     * @return The newly created {@link Creator}.
     */
    public abstract Creator getCreator(Creator.Context context, IPPlayer player, @Nullable String name);

    public @Nullable AudioSet getAudioSet()
    {
        return null;
    }

    @Override
    public final String toString()
    {
        return getPluginName() + ":" + getSimpleName() + ":" + getTypeVersion();
    }

    @Override
    public final int hashCode()
    {
        // There may only ever exist 1 instance of each MovableType.
        return super.hashCode();
    }

    @Override
    public final boolean equals(@Nullable Object obj)
    {
        // There may only ever exist 1 instance of each MovableType.
        return super.equals(obj);
    }
}
