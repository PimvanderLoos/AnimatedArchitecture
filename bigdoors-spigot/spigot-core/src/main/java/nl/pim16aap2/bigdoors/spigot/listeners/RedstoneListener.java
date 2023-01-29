package nl.pim16aap2.bigdoors.spigot.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.managers.PowerBlockManager;
import nl.pim16aap2.bigdoors.spigot.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.spigot.util.implementations.PLocationSpigot;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
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
@Flogger
public class RedstoneListener extends AbstractListener
{
    private final ConfigLoaderSpigot config;
    private final Set<Material> powerBlockTypes = new HashSet<>();
    private final PowerBlockManager powerBlockManager;

    @Inject
    public RedstoneListener(
        RestartableHolder holder, JavaPlugin plugin, ConfigLoaderSpigot config, PowerBlockManager powerBlockManager)
    {
        super(holder, plugin, config::isRedstoneEnabled);
        this.config = config;
        this.powerBlockManager = powerBlockManager;
    }

    @Override
    public void initialize()
    {
        super.initialize();
        if (super.isRegistered)
            powerBlockTypes.addAll(config.powerBlockTypes());
    }

    @Override
    public void shutDown()
    {
        super.shutDown();
        powerBlockTypes.clear();
    }

    private void checkMovables(PLocationSpigot loc, boolean isPowered)
    {
        powerBlockManager
            .movablesFromPowerBlockLoc(loc)
            .thenAccept(movables -> movables.forEach(movable -> movable.onRedstoneChange(isPowered)))
            .exceptionally(Util::exceptionally);
    }

    /**
     * Processes a redstone event. This means that it looks for any power blocks around the block that was changed.
     *
     * @param event
     *     The event to process.
     */
    private void processRedstoneEvent(BlockRedstoneEvent event)
    {
        final boolean isPowered = event.getNewCurrent() > 0;
        try
        {
            final Block block = event.getBlock();
            final Location location = block.getLocation();
            final World world = Objects.requireNonNull(location.getWorld(), "World cannot be null!");

            final int x = location.getBlockX();
            final int y = location.getBlockY();
            final int z = location.getBlockZ();

            if (powerBlockTypes.contains(world.getBlockAt(x, y, z - 1).getType())) // North
                checkMovables(new PLocationSpigot(world, x, y, z - 1.0), isPowered);

            if (powerBlockTypes.contains(world.getBlockAt(x + 1, y, z).getType())) // East
                checkMovables(new PLocationSpigot(world, x + 1.0, y, z), isPowered);

            if (powerBlockTypes.contains(world.getBlockAt(x, y, z + 1).getType())) // South
                checkMovables(new PLocationSpigot(world, x, y, z + 1.0), isPowered);

            if (powerBlockTypes.contains(world.getBlockAt(x - 1, y, z).getType())) // West
                checkMovables(new PLocationSpigot(world, x - 1.0, y, z), isPowered);

            if (powerBlockTypes.contains(world.getBlockAt(x, y + 1, z).getType())) // Above
                checkMovables(new PLocationSpigot(world, x, y + 1.0, z), isPowered);

            if (powerBlockTypes.contains(world.getBlockAt(x, y - 1, z).getType())) // Under
                checkMovables(new PLocationSpigot(world, x, y - 1.0, z), isPowered);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Exception thrown while handling redstone event!");
        }
    }

    /**
     * Listens to redstone changes and checks if there are any movables attached to it. Any movables that are found are
     * then toggled, if possible.
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

        if (!powerBlockManager.isBigDoorsWorld(event.getBlock().getWorld().getName()))
            return;

        CompletableFuture.runAsync(() -> processRedstoneEvent(event)).exceptionally(Util::exceptionally);
    }
}
