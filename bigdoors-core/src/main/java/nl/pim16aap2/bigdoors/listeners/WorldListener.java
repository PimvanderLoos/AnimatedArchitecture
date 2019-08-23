package nl.pim16aap2.bigdoors.listeners;

import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Process world (un)load events.
 *
 * @author Pim
 */
public class WorldListener implements Listener
{
    private static WorldListener instance;

    @NotNull
    private final PowerBlockManager powerBlockManager;

    private WorldListener(final @NotNull PowerBlockManager powerBlockManager)
    {
        this.powerBlockManager = powerBlockManager;
        for (World world : Bukkit.getWorlds())
            powerBlockManager.loadWorld(world.getUID());
    }

    /**
     * Initializes the {@link WorldListener}. If it has already been initialized, it'll return that instance instead.
     *
     * @param powerBlockManager The {@link PowerBlockManager}.
     * @return The instance of this {@link WorldListener}.
     */
    public static WorldListener init(final @NotNull PowerBlockManager powerBlockManager)
    {
        return instance == null ? instance = new WorldListener(powerBlockManager) : instance;
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldLoad(final WorldLoadEvent event)
    {
        powerBlockManager.loadWorld(event.getWorld().getUID());
    }

    @EventHandler(ignoreCancelled = true)
    public void onWorldUnload(final WorldUnloadEvent event)
    {
        powerBlockManager.unloadWorld(event.getWorld().getUID());
    }

}
