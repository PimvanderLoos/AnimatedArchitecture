package nl.pim16aap2.bigdoors.listeners;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.config.ConfigLoaderSpigot;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionEventSpigot;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Restartable;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a listener that keeps track redstone changes.
 *
 * @author Pim
 */
public class RedstoneListener extends Restartable implements Listener
{
    private final BigDoorsSpigot plugin;
    private final Set<Material> powerBlockTypes = new HashSet<>();

    public RedstoneListener(final @NotNull BigDoorsSpigot plugin)
    {
        super(plugin);
        this.plugin = plugin;
        restart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
        powerBlockTypes.clear();
        IConfigLoader config = plugin.getConfigLoader();
        if (!(config instanceof ConfigLoaderSpigot))
        {
            PLogger.get().logException(new IllegalStateException(
                "Config loader is not a Spigot config loader! Defaulting to gold block power block!"));
            powerBlockTypes.add(Material.GOLD_BLOCK);
            return;
        }
        ConfigLoaderSpigot spigotConfig = (ConfigLoaderSpigot) config;
        powerBlockTypes.addAll(spigotConfig.powerBlockTypes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        powerBlockTypes.clear();
    }

    private void checkDoors(final @NotNull Location loc)
    {
        BigDoors.get().getPowerBlockManager().doorsFromPowerBlockLoc(
            new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getWorld().getUID()).whenComplete(
            (doorList, throwable) -> doorList.forEach(
                door ->
                {
                    // TODO: Less stupid system.
                    CompletableFuture<Optional<AbstractDoorBase>> futureDoor = CompletableFuture
                        .completedFuture(Optional.of(door));
                    plugin.callDoorActionEvent(new DoorActionEventSpigot(futureDoor, DoorActionCause.REDSTONE,
                                                                         DoorActionType.TOGGLE, null));
                }));
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
            plugin.getPLogger().logException(e, "Exception thrown while handling redstone event!");
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

        if (!BigDoors.get().getPowerBlockManager().isBigDoorsWorld(event.getBlock().getWorld().getUID()))
            return;

        CompletableFuture.runAsync(() -> processRedstoneEvent(event));
    }
}
