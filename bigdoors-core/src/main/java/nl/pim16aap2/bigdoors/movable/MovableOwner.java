package nl.pim16aap2.bigdoors.movable;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;

/**
 * Contains all details needed about the owner of a Movable.
 *
 * @param movableUID
 *     The UID of the movable that is owned.
 * @param permission
 *     The {@link PermissionLevel} level at which the player owns the movable.
 * @param pPlayerData
 *     The {@link IPPlayer} object represented by this {@link MovableOwner}.
 * @author Pim
 */
public record MovableOwner(long movableUID, PermissionLevel permission, PPlayerData pPlayerData)
{
}
