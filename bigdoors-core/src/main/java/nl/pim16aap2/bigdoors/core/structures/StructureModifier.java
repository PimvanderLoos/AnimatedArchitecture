package nl.pim16aap2.bigdoors.core.structures;

import nl.pim16aap2.bigdoors.core.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Class that can modify some protected aspects of structures.
 */
public final class StructureModifier
{
    private static final StructureModifier INSTANCE = new StructureModifier();

    private StructureModifier()
    {
    }

    /**
     * Obtains the instance of this class.
     * <p>
     * Note that only the {@link DatabaseManager} is allowed to get an instance at this time. This is for good reason,
     * as using this object through other means can leave the structure in an incorrect state. So, if you think you need
     * to access this class, please think again and again until you realise that you do not.
     * <p>
     * Instead, use the publicly accessible methods in the database manager and the structure classes.
     *
     * @param friendKey
     *     The friend key of the database manager class. This is used to verify that the caller is allowed to obtain
     *     access to this class.
     * @return The instance of this class.
     */
    public static StructureModifier get(DatabaseManager.FriendKey friendKey)
    {
        Util.requireNonNull(friendKey, "friendKey");
        return INSTANCE;
    }

    /**
     * Adds an owner to a structure.
     *
     * @param structure
     *     The structure to add the owner to.
     * @param structureOwner
     *     The new owner of the door.
     * @return True if the owner was removed.
     */
    public boolean addOwner(AbstractStructure structure, StructureOwner structureOwner)
    {
        return structure.addOwner(structureOwner);
    }

    /**
     * Removes an owner from this structure.
     *
     * @param structure
     *     The structure to remove the owner from.
     * @param ownerUUID
     *     The UUID of the owner to remove.
     * @return The owner that was removed. If no owner with that UUID exists, or if that specific owner could not be
     * removed, the result will be null.
     */
    public @Nullable StructureOwner removeOwner(AbstractStructure structure, UUID ownerUUID)
    {
        return structure.removeOwner(ownerUUID);
    }
}
