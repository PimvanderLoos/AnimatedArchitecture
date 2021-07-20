package nl.pim16aap2.bigDoors.skulls;

import java.lang.reflect.Field;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonObject;

import nl.pim16aap2.bigDoors.util.TimedCache;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class HeadManager
{
    protected final JavaPlugin plugin;
    protected TimedCache<UUID, ItemStack> headMap;
    protected TimedCache<String, JsonObject> map;

    protected HeadManager(JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    public void init(int headCacheTimeout)
    {
        map = new TimedCache<>(plugin, headCacheTimeout);
        headMap = new TimedCache<>(plugin, headCacheTimeout);
    }

    public void reload(int headCacheTimeout)
    {
        headMap.reinit(headCacheTimeout);
        map.reinit(headCacheTimeout);
    }

    public ItemStack getPlayerHead(UUID playerUUID, String playerName, int x, int y, int z, Player player)
    {
        if (headMap.contains(playerUUID))
            return headMap.get(playerUUID);

        createSkull(x, y, z, playerName, playerUUID, player);
        return headMap.get(playerUUID);
    }

    protected abstract void createSkull(int x, int y, int z, String name, UUID playerUUID, Player p);
}
