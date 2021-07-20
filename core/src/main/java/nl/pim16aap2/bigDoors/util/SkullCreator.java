package nl.pim16aap2.bigDoors.util;

import com.cryptomorin.xseries.SkullUtils;
import nl.pim16aap2.bigDoors.skulls.HeadManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class SkullCreator extends HeadManager
{
    public SkullCreator(JavaPlugin plugin)
    {
        super(plugin);
    }

    @Override
    protected void createSkull(int x, int y, int z, String name, UUID playerUUID, Player p)
    {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
        {
            final ItemStack skull = SkullUtils.getSkull(playerUUID);
            final SkullMeta sm = (SkullMeta) skull.getItemMeta();

            sm.setDisplayName(name);
            skull.setItemMeta(sm);

            headMap.put(playerUUID, skull);
        });
    }
}
