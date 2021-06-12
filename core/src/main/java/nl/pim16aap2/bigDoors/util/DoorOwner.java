package nl.pim16aap2.bigDoors.util;

import java.util.UUID;

import javax.annotation.Nullable;

import nl.pim16aap2.bigDoors.BigDoors;

public class DoorOwner
{
    private UUID playerUUID;
    private long doorUID;
    private int permission;
    private String name;
    private final BigDoors plugin;

    public DoorOwner(BigDoors plugin, long doorUID, UUID playerUUID, int permission, @Nullable String name)
    {
        this.plugin = plugin;
        this.doorUID = doorUID;
        this.playerUUID = playerUUID;
        this.permission = permission;
        this.name = name;
    }

    public long getDoorUID()
    {
        return doorUID;
    }

    public UUID getPlayerUUID()
    {
        return playerUUID;
    }

    public int getPermission()
    {
        return permission;
    }

    public String getPlayerName()
    {
        if (name == null)
            name = plugin.getCommander().playerNameFromUUID(playerUUID);
        return name;
    }
}