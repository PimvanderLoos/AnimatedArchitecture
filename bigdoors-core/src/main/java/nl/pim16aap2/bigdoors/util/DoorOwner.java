package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Contains all details needed about a doorOwner.
 *
 * @author Pim
 */
public class DoorOwner
{
    /**
     * The UID of the door that is owned.
     */
    private final long doorUID;

    /**
     * The permission level at which the player owns the door.
     */
    private final int permission;

    /**
     * The {@link IPPlayer} object represented by this {@link DoorOwner}.
     */
    private final IPPlayer player;

    /**
     * Constructor of {@link DoorOwner}.
     *
     * @param doorUID    The UID of the DoorBase.
     * @param permission The permission level at which the player owns the door.
     * @param player     The playuer that owns the given door.
     */
    public DoorOwner(final long doorUID, final int permission, final @NotNull IPPlayer player)
    {
        this.doorUID = doorUID;
        this.permission = permission;
        this.player = player;
    }

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
     * Get the UID of the door.
     *
     * @return the UID of the door.
     */
    public long getDoorUID()
    {
        return doorUID;
    }

    /**
     * Gets the {@link IPPlayer}.
     *
     * @return The {@link IPPlayer}.
     */
    @NotNull
    public IPPlayer getPlayer()
    {
        return player;
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
     * Get the permission level of the owner of the door.
     *
     * @return The permission level of the owner.
     */
    public int getPermission()
    {
        return permission;
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

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DoorOwner other = (DoorOwner) o;
        return doorUID == other.doorUID && getPlayerUUID().equals(other.getPlayerUUID()) &&
            permission == other.permission && getPlayerName().equals(other.getPlayerName());
    }
}
