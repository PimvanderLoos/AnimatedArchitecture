package nl.pim16aap2.bigdoors.spigotutil;

import java.util.UUID;

/**
 * Contains all details needed about a doorOwner.
 *
 * @author Pim
 */
public class DoorOwner
{
    private UUID playerUUID;
    private long doorUID;
    private int permission;
    private String playerName;

    /**
     * Constructor of {@link DoorOwner}.
     * 
     * @param doorUID    The UID of the DoorBase.
     * @param playerUUID The UUID of the player that owns the given door.
     * @param playerName The name of the player that owns the given door.
     * @param permission The permission level at which the player owns the door.
     */
    public DoorOwner(long doorUID, UUID playerUUID, String playerName, int permission)
    {
        this.doorUID = doorUID;
        this.playerUUID = playerUUID;
        this.permission = permission;
        this.playerName = playerName;
        if (playerUUID == null)
            throw new NullPointerException();
        if (playerName == null)
            throw new NullPointerException();
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
    public String getPlayerName()
    {
        return playerName;
    }

    /**
     * Get a basic overview of this door owner. Useful for debugging.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("doorUID: ");
        sb.append(doorUID);

        sb.append(". playerUUID: ");
        sb.append(playerUUID.toString());

        sb.append(". Permission: ");
        sb.append(permission);

        sb.append(". PlayerName: ");
        sb.append(playerName);
        return sb.toString();
    }
}
