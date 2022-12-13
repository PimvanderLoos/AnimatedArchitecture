package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;

/**
 * Contains all details needed about a doorOwner.
 *
 * @param doorUID
 *     The UID of the door that is owned.
 * @param permission
 *     The {@link PermissionLevel} level at which the player owns the door.
 * @param pPlayerData
 *     The {@link IPPlayer} object represented by this {@link DoorOwner}.
 * @author Pim
 */
public record DoorOwner(long doorUID, PermissionLevel permission, PPlayerData pPlayerData)
{
}
