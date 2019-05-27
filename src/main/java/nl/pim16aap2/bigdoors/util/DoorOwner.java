package nl.pim16aap2.bigdoors.util;

import java.util.UUID;

import javax.annotation.Nullable;

import nl.pim16aap2.bigdoors.BigDoors;

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
            name = plugin.getDatabaseManager().playerNameFromUUID(playerUUID);
        return name;
    }

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

        sb.append(". Name: ");
        sb.append(name);
        return sb.toString();
    }
}