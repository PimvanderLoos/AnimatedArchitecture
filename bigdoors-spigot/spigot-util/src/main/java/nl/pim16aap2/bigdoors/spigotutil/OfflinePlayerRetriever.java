package nl.pim16aap2.bigdoors.spigotutil;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class OfflinePlayerRetriever
{
    public OfflinePlayer getPlayer(UUID uuid)
    {
        return Bukkit.getOfflinePlayer(uuid);
    }
}
