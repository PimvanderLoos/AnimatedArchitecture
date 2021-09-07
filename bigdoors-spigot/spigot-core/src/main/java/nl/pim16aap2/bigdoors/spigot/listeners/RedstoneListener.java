package nl.pim16aap2.bigdoors.spigot.listeners;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a listener that keeps track redstone changes.
 *
 * @author Pim
 */
@Singleton
public class RedstoneListener extends Restartable implements Listener
{
    private final JavaPlugin plugin;
    private final ConfigLoaderSpigot config;
    private final IPLogger logger;
    private final Set<Material> powerBlockTypes = new HashSet<>();
    private boolean isRegistered = false;

    @Inject
    public RedstoneListener(IRestartableHolder holder, JavaPlugin plugin, ConfigLoaderSpigot config, IPLogger logger)
    {
        super(holder);
        this.plugin = plugin;
        this.config = config;
        this.logger = logger;
        restart();
    }

    @Override
    public void restart()
    {
        powerBlockTypes.clear();

        if (config.enableRedstone())
        {
            register();
            powerBlockTypes.addAll(config.powerBlockTypes());
            return;
        }
        unregister();
    }

    /**
     * Registers this listener if it isn't already registered.
     */
    private void register()
    {
        if (isRegistered)
            return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        isRegistered = true;
    }

    /**
     * Unregisters this listener if it isn't already unregistered.
     */
    private void unregister()
    {
        if (!isRegistered)
            return;
        HandlerList.unregisterAll(this);
        isRegistered = false;
    }

    @Override
    public void shutdown()
    {
        powerBlockTypes.clear();
        unregister();
    }

    private void checkDoors(Location loc)
    {
        final String worldName = Objects.requireNonNull(loc.getWorld(), "World cannot be null!").getName();
        BigDoors.get().getPlatform().getPowerBlockManager().doorsFromPowerBlockLoc(
            new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), worldName).whenComplete(
            (doorList, throwable) -> doorList.forEach(
                door -> BigDoors.get().getDoorOpener()
                                .animateDoorAsync(door, DoorActionCause.REDSTONE, null, 0, false,
                                                  DoorActionType.TOGGLE)));
    }

    /**
     * Processes a redstone event. This means that it looks for any power blocks around the block that was changed.
     *
     * @param event
     *     The event.
     */
    private void processRedstoneEvent(BlockRedstoneEvent event)
    {
        try
        {
            final Block block = event.getBlock();
            final Location location = block.getLocation();
            final World world = Objects.requireNonNull(location.getWorld(), "World cannot be null!");

            final int x = location.getBlockX();
            final int y = location.getBlockY();
            final int z = location.getBlockZ();

            if (powerBlockTypes.contains(world.getBlockAt(x, y, z - 1).getType())) // North
                checkDoors(new Location(world, x, y, z - 1.0));

            if (powerBlockTypes.contains(world.getBlockAt(x + 1, y, z).getType())) // East
                checkDoors(new Location(world, x + 1.0, y, z));

            if (powerBlockTypes.contains(world.getBlockAt(x, y, z + 1).getType())) // South
                checkDoors(new Location(world, x, y, z + 1.0));

            if (powerBlockTypes.contains(world.getBlockAt(x - 1, y, z).getType())) // West
                checkDoors(new Location(world, x - 1.0, y, z));

            if (y < 254 && powerBlockTypes.contains(world.getBlockAt(x, y + 1, z).getType())) // Above
                checkDoors(new Location(world, x, y + 1.0, z));

            if (y > 0 && powerBlockTypes.contains(world.getBlockAt(x, y - 1, z).getType())) // Under
                checkDoors(new Location(world, x, y - 1.0, z));
        }
        catch (Exception e)
        {
            logger.logThrowable(e, "Exception thrown while handling redstone event!");
        }
    }

    /**
     * Listens to redstone changes and checks if there are any doors attached to it. Any doors that are found are then
     * toggled, if possible.
     *
     * @param event
     *     The {@link BlockRedstoneEvent}.
     */
    @EventHandler
    public void onBlockRedstoneChange(BlockRedstoneEvent event)
    {
        // Only boolean status is allowed, so a varying degree of "on" has no effect.
        if (event.getOldCurrent() != 0 && event.getNewCurrent() != 0)
            return;

        if (!BigDoors.get().getPlatform().getPowerBlockManager().isBigDoorsWorld(event.getBlock().getWorld().getName()))
            return;

        CompletableFuture.runAsync(() -> processRedstoneEvent(event)).exceptionally(Util::exceptionally);
    }
}
