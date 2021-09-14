package nl.pim16aap2.bigdoors.spigot.listeners;

import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Process world (un)load events.
 *
 * @author Pim
 */
@Singleton
public final class WorldListener extends AbstractListener
{
    private final PowerBlockManager powerBlockManager;

    @Inject
    public WorldListener(JavaPlugin plugin, PowerBlockManager powerBlockManager)
    {
        super(plugin);
        this.powerBlockManager = powerBlockManager;
        for (final World world : Bukkit.getWorlds())
            powerBlockManager.loadWorld(world.getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldLoad(WorldLoadEvent event)
    {
        powerBlockManager.loadWorld(event.getWorld().getName());
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event)
    {
        powerBlockManager.unloadWorld(event.getWorld().getName());
    }

}
