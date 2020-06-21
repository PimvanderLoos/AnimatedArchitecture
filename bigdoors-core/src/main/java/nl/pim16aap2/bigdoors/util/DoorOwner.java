package nl.pim16aap2.bigdoors.util;

import lombok.AllArgsConstructor;
import lombok.Value;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Contains all details needed about a doorOwner.
 *
 * @author Pim
 */
@Value
@AllArgsConstructor
public class DoorOwner
{
    /**
     * The UID of the door that is owned.
     */
    long doorUID;

    /**
     * The permission level at which the player owns the door.
     */
    int permission;

    /**
     * The {@link IPPlayer} object represented by this {@link DoorOwner}.
     */
    @NotNull
    IPPlayer player;

    /**
     * Constructor of {@link DoorOwner}.
     *
     * @param doorUID    The UID of the DoorBase.
     * @param playerUUID The UUID of the player that owns the given door.
     * @param playerName The name of the player that owns the given door.
     * @param permission The permission level at which the player owns the door.
     */
    public DoorOwner(final long doorUID, final @NotNull UUID playerUUID, final @NotNull String playerName,
                     final int permission)
    {
        this(doorUID, permission, BigDoors.get().getPlatform().getPPlayerFactory().create(playerUUID, playerName));
    }

    /**
     * Get the UUID of the player.
     *
     * @return The UUID of the player.
     */
    @NotNull
    public UUID getPlayerUUID()
    {
        return player.getUUID();
    }

    /**
     * Get the name of the player.
     *
     * @return The name of the player.
     */
    @NotNull
    public String getPlayerName()
    {
        return player.getName();
    }

    /**
     * Get a basic overview of this door owner. Useful for debugging.
     */
    @NotNull
    @Override
    public String toString()
    {
        return "doorUID: " + doorUID +
            ". playerUUID: " + getPlayerUUID().toString() +
            ". Permission: " + permission +
            ". PlayerName: " + getPlayerName();
    }
}
