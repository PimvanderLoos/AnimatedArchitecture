package nl.pim16aap2.bigdoors.spigotutil;

import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.UUID;

public class WorldRetriever
{
    public World worldFromString(UUID uuid)
    {
        return Bukkit.getServer().getWorld(uuid);
    }
}
