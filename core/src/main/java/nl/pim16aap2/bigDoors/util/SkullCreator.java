package nl.pim16aap2.bigDoors.util;

import nl.pim16aap2.bigDoors.skulls.HeadManager;
import nl.pim16aap2.bigDoors.skulls.Skull;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class SkullCreator extends HeadManager
{
    public SkullCreator(JavaPlugin plugin)
    {
        super(plugin);
    }

    @Override
    protected String[] getFromPlayer(Player playerBukkit)
    {
        return new String[0];
    }

    @Override
    protected void createSkull(int x, int y, int z, String name, UUID playerUUID, Player p)
    {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
        {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);

        });
    }

    @Override
    protected String[] getFromName(String name, Player p)
    {
        return new String[0];
    }
}
