package nl.pim16aap2.bigdoors.util;

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
     * The {@link UUID} of the player that owns this door.
     */
    private final UUID playerUUID;

    /**
     * The name of the player that owns this door.
     */
    private final String playerName;

    /**
     * The UID of the door that is owned.
     */
    private final long doorUID;

    /**
     * The permission level at which the player owns the door.
     */
    private final int permission;

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
        this.doorUID = doorUID;
        this.playerUUID = playerUUID;
        this.permission = permission;
        this.playerName = playerName;
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
     * Get the UUID of the player.
     *
     * @return The UUID of the player.
     */
    @NotNull
    public UUID getPlayerUUID()
    {
        return playerUUID;
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
        return playerName;
    }

    /**
     * Get a basic overview of this door owner. Useful for debugging.
     */
    @NotNull
    @Override
    public String toString()
    {
        return "doorUID: " + doorUID +
            ". playerUUID: " + playerUUID.toString() +
            ". Permission: " + permission +
            ". PlayerName: " + playerName;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DoorOwner other = (DoorOwner) o;
        return doorUID == other.doorUID && playerUUID.equals(other.playerUUID) &&
            permission == other.permission && playerName.equals(other.playerName);
    }
}
