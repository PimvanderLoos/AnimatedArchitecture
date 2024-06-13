package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebuggableRegistry;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.IDebuggable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.managers.PowerBlockManager;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.core.config.ConfigSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.implementations.LocationSpigot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Represents a listener that keeps track redstone changes.
 */
@Singleton
@Flogger
public class RedstoneListener extends AbstractListener implements IDebuggable
{
    private final ConfigSpigot config;
    private final Set<Material> powerBlockTypes = new CopyOnWriteArraySet<>();
    private final PowerBlockManager powerBlockManager;

    /**
     * The thread pool to use for handling redstone events.
     */
    private volatile @Nullable ExecutorService threadPool;

    @Inject RedstoneListener(
        RestartableHolder holder,
        JavaPlugin plugin,
        ConfigSpigot config,
        PowerBlockManager powerBlockManager,
        DebuggableRegistry debuggableRegistry)
    {
        super(holder, plugin, config::isRedstoneEnabled);
        this.config = config;
        this.powerBlockManager = powerBlockManager;

        debuggableRegistry.registerDebuggable(this);
    }

    @Override
    public void initialize()
    {
        initThreadPool();
        super.initialize();
        if (super.isRegistered)
            powerBlockTypes.addAll(config.powerBlockTypes());
    }

    @Override
    public void shutDown()
    {
        super.shutDown();
        powerBlockTypes.clear();
        shutDownThreadPool();
    }

    private void checkStructures(LocationSpigot loc, boolean isPowered)
    {
        powerBlockManager
            .structuresFromPowerBlockLoc(loc)
            .thenAccept(structures -> structures.forEach(structure -> structure.onRedstoneChange(isPowered)))
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
                checkStructures(new LocationSpigot(world, x, y, z - 1.0), isPowered);

            if (powerBlockTypes.contains(world.getBlockAt(x + 1, y, z).getType())) // East
                checkStructures(new LocationSpigot(world, x + 1.0, y, z), isPowered);

            if (powerBlockTypes.contains(world.getBlockAt(x, y, z + 1).getType())) // South
                checkStructures(new LocationSpigot(world, x, y, z + 1.0), isPowered);

            if (powerBlockTypes.contains(world.getBlockAt(x - 1, y, z).getType())) // West
                checkStructures(new LocationSpigot(world, x - 1.0, y, z), isPowered);

            if (powerBlockTypes.contains(world.getBlockAt(x, y + 1, z).getType())) // Above
                checkStructures(new LocationSpigot(world, x, y + 1.0, z), isPowered);

            if (powerBlockTypes.contains(world.getBlockAt(x, y - 1, z).getType())) // Under
                checkStructures(new LocationSpigot(world, x, y - 1.0, z), isPowered);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Exception thrown while handling redstone event!");
        }
    }

    /**
     * Listens to redstone changes and checks if there are any structures attached to it. Any structures that are found
     * are then toggled, if possible.
     *
     * @param event
     *     The {@link BlockRedstoneEvent}.
     */
    @EventHandler
    public void onBlockRedstoneChange(BlockRedstoneEvent event)
    {
        final @Nullable ExecutorService currentThreadPool = this.threadPool;
        if (currentThreadPool == null)
        {
            log.atWarning().log(
                "Redstone event at location %s was not processed because the thread pool was null!",
                event.getBlock().getLocation());
            return;
        }
        if (currentThreadPool.isShutdown())
        {
            log.atWarning().log(
                "Redstone event at location %s was not processed because the thread pool was shut down!",
                event.getBlock().getLocation());
            return;
        }

        // Only boolean status is allowed, so a varying degree of "on" has no effect.
        if (event.getOldCurrent() != 0 && event.getNewCurrent() != 0)
            return;

        if (!powerBlockManager.isAnimatedArchitectureWorld(event.getBlock().getWorld().getName()))
            return;

        CompletableFuture
            .runAsync(() -> processRedstoneEvent(event), currentThreadPool)
            .orTimeout(5, TimeUnit.SECONDS)
            .exceptionally(
                exception ->
                {
                    if (exception instanceof TimeoutException)
                    {
                        log.atWarning().log(
                            "Timed out processing redstone event at location %s: '%s'",
                            event.getBlock().getLocation(),
                            exception.getMessage());
                    }
                    else
                    {
                        Util.exceptionally(exception);
                    }
                    return null;
                });
    }

    private synchronized void initThreadPool()
    {
        if (threadPool != null)
            throw new IllegalStateException("Thread pool is already initialized!");
        threadPool = Executors.newFixedThreadPool(config.redstoneThreadPoolSize());
    }

    private synchronized void shutDownThreadPool()
    {
        final @Nullable ExecutorService currentThreadPool = this.threadPool;
        this.threadPool = null;

        if (currentThreadPool != null)
        {
            currentThreadPool.shutdown();
            try
            {
                if (!currentThreadPool.awaitTermination(30, TimeUnit.SECONDS))
                    log.atSevere().log(
                        "Timed out waiting to terminate DatabaseManager ExecutorService!" +
                            " The database may be out of sync with the world!");
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new RuntimeException(
                    "Thread got interrupted waiting for DatabaseManager ExecutorService to terminate!" +
                        " The database may be out of sync with the world!", e);
            }
        }
    }

    @Override
    public String getDebugInformation()
    {
        return String.format(
            "Listener status: registered=%s, powerBlockTypes=%s, threadPool=%s",
            super.isRegistered,
            powerBlockTypes,
            threadPool);
    }
}
