package nl.pim16aap2.bigdoors.spigotutil;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class PlayerRetriever
{
    public OfflinePlayer getOfflinePlayer(UUID uuid)
    {
        return Bukkit.getOfflinePlayer(uuid);
    }

    public String nameFromUUID(UUID uuid)
    {
        return SpigotUtil.nameFromUUID(uuid);
    }
}
