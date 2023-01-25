package nl.pim16aap2.bigdoors.movable;

import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Class that can modify some protected aspects of movables.
 */
public final class MovableModifier
{
    private static final MovableModifier INSTANCE = new MovableModifier();

    private MovableModifier()
    {
    }

    /**
     * Obtains the instance of this class.
     * <p>
     * Note that only the {@link DatabaseManager} is allowed to get an instance at this time. This is for good reason,
     * as using this object through other means can leave the movable in an incorrect state. So, if you think you need
     * to access this class, please think again and again until you realise that you do not.
     * <p>
     * Instead, use the publicly accessible methods in the database manager and the movable classes.
     *
     * @param friendKey
     *     The friend key of the database manager class. This is used to verify that the caller is allowed to obtain
     *     access to this class.
     * @return The instance of this class.
     */
    public static MovableModifier get(DatabaseManager.FriendKey friendKey)
    {
        Util.requireNonNull(friendKey, "friendKey");
        return INSTANCE;
    }

    /**
     * Adds an owner to a movable.
     *
     * @param movable
     *     The movable to add the owner to.
     * @param movableOwner
     *     The new owner of the door.
     * @return True if the owner was removed.
     */
    public boolean addOwner(AbstractMovable movable, MovableOwner movableOwner)
    {
        return movable.addOwner(movableOwner);
    }

    /**
     * Removes an owner from this movable.
     *
     * @param movable
     *     The movable to remove the owner from.
     * @param ownerUUID
     *     The UUID of the owner to remove.
     * @return The owner that was removed. If no owner with that UUID exists, or if that specific owner could not be
     * removed, the result will be null.
     */
    public @Nullable MovableOwner removeOwner(AbstractMovable movable, UUID ownerUUID)
    {
        return movable.removeOwner(ownerUUID);
    }
}
