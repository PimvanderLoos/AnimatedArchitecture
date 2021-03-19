package nl.pim16aap2.bigdoors.spigot.listeners;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a listener that keeps track redstone changes.
 *
 * @author Pim
 */
public class RedstoneListener extends Restartable implements Listener
{
    @Nullable
    private static RedstoneListener INSTANCE;
    @NotNull
    private final BigDoorsSpigot plugin;
    @NotNull
    private final Set<Material> powerBlockTypes = new HashSet<>();
    private boolean isRegistered = false;

    private RedstoneListener(final @NotNull BigDoorsSpigot plugin)
    {
        super(plugin);
        this.plugin = plugin;
        restart();
    }

    /**
     * Initializes the {@link RedstoneListener}. If it has already been initialized, it'll return that instance
     * instead.
     *
     * @param plugin The {@link BigDoorsSpigot} plugin.
     * @return The instance of this {@link RedstoneListener}.
     */
    public static @NotNull RedstoneListener init(final @NotNull BigDoorsSpigot plugin)
    {
        return (INSTANCE == null) ? INSTANCE = new RedstoneListener(plugin) : INSTANCE;
    }

    @Override
    public void restart()
    {
        powerBlockTypes.clear();

        if (plugin.getConfigLoader().enableRedstone())
        {
            register();
            powerBlockTypes.addAll(plugin.getConfigLoader().powerBlockTypes());
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

    private void checkDoors(final @NotNull Location loc)
    {
        BigDoors.get().getPowerBlockManager().doorsFromPowerBlockLoc(
            new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getWorld().getName()).whenComplete(
            (doorList, throwable) -> doorList.forEach(
                door -> DoorOpener.get().animateDoorAsync(door, DoorActionCause.REDSTONE, null, 0, false,
                                                          DoorActionType.TOGGLE)));
    }

    /**
     * Processes a redstone event. This means that it looks for any power blocks around the block that was changed.
     *
     * @param event The event.
     */
    private void processRedstoneEvent(final @NotNull BlockRedstoneEvent event)
    {
        try
        {
            Block block = event.getBlock();
            Location location = block.getLocation();
            int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();

            if (powerBlockTypes.contains(location.getWorld().getBlockAt(x, y, z - 1).getType())) // North
                checkDoors(new Location(location.getWorld(), x, y, z - 1));

            if (powerBlockTypes.contains(location.getWorld().getBlockAt(x + 1, y, z).getType())) // East
                checkDoors(new Location(location.getWorld(), x + 1, y, z));

            if (powerBlockTypes.contains(location.getWorld().getBlockAt(x, y, z + 1).getType())) // South
                checkDoors(new Location(location.getWorld(), x, y, z + 1));

            if (powerBlockTypes.contains(location.getWorld().getBlockAt(x - 1, y, z).getType())) // West
                checkDoors(new Location(location.getWorld(), x - 1, y, z));

            if (y < 254 && powerBlockTypes.contains(location.getWorld().getBlockAt(x, y + 1, z).getType())) // Above
                checkDoors(new Location(location.getWorld(), x, y + 1, z));

            if (y > 0 && powerBlockTypes.contains(location.getWorld().getBlockAt(x, y - 1, z).getType())) // Under
                checkDoors(new Location(location.getWorld(), x, y - 1, z));
        }
        catch (Exception e)
        {
            plugin.getPLogger().logThrowable(e, "Exception thrown while handling redstone event!");
        }
    }

    /**
     * Listens to redstone changes and checks if there are any doors attached to it. Any doors that are found are then
     * toggled, if possible.
     *
     * @param event The {@link BlockRedstoneEvent}.
     */
    @EventHandler
    public void onBlockRedstoneChange(final @NotNull BlockRedstoneEvent event)
    {
        // Only boolean status is allowed, so a varying degree of "on" has no effect.
        if (event.getOldCurrent() != 0 && event.getNewCurrent() != 0)
            return;

        if (!BigDoors.get().getPowerBlockManager().isBigDoorsWorld(event.getBlock().getWorld().getName()))
            return;

        CompletableFuture.runAsync(() -> processRedstoneEvent(event)).exceptionally(Util::exceptionally);
    }
}
