package nl.pim16aap2.bigdoors.structures;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;

/**
 * Contains all details needed about the owner of a Structure.
 *
 * @param structureUID
 *     The UID of the structure that is owned.
 * @param permission
 *     The {@link PermissionLevel} level at which the player owns the structure.
 * @param pPlayerData
 *     The {@link IPPlayer} object represented by this {@link StructureOwner}.
 * @author Pim
 */
public record StructureOwner(long structureUID, PermissionLevel permission, PPlayerData pPlayerData)
{
}
