package nl.pim16aap2.bigdoors.core.structures;

import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.PlayerData;

/**
 * Contains all details needed about the owner of a Structure.
 *
 * @param structureUID
 *     The UID of the structure that is owned.
 * @param permission
 *     The {@link PermissionLevel} level at which the player owns the structure.
 * @param playerData
 *     The {@link IPlayer} object represented by this {@link StructureOwner}.
 * @author Pim
 */
public record StructureOwner(long structureUID, PermissionLevel permission, PlayerData playerData)
{
}
