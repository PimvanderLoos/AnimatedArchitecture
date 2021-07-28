package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import org.jetbrains.annotations.NotNull;

/**
 * Contains all details needed about a doorOwner.
 *
 * @param doorUID     The UID of the door that is owned.
 * @param permission  The permission level at which the player owns the door.
 * @param pPlayerData The {@link IPPlayer} object represented by this {@link DoorOwner}.
 * @author Pim
 */
public record DoorOwner(long doorUID, int permission, @NotNull PPlayerData pPlayerData)
{
}
