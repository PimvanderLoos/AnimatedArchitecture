package nl.pim16aap2.bigdoors.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Contains all details needed about a doorOwner.
 *
 * @author Pim
 */
@AllArgsConstructor
public class DoorOwner
{
    /**
     * The UID of the door that is owned.
     */
    @Getter
    long doorUID;

    /**
     * The permission level at which the player owns the door.
     */
    @Getter
    int permission;

    /**
     * The {@link IPPlayer} object represented by this {@link DoorOwner}.
     */
    @Getter
    @NotNull IPPlayer player;

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
     * Get a basic overview of this door owner. Useful for debugging.
     */
    @Override
    public @NotNull String toString()
    {
        return "doorUID: " + doorUID + ". player: " + getPlayer().toString() + ". Permission: " + permission;
    }

    @Override
    public final int hashCode()
    {
        return player.getUUID().hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        DoorOwner other = (DoorOwner) o;
        return player.equals(other.player) && doorUID == other.doorUID && permission == other.permission;
    }
}
