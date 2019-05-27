package nl.pim16aap2.bigdoors.compatiblity;

import java.lang.reflect.Method;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import com.intellectualcrafters.plot.api.PlotAPI;
import com.intellectualcrafters.plot.object.Plot;
import com.plotsquared.bukkit.listeners.PlayerEvents;

import nl.pim16aap2.bigdoors.BigDoors;

class PlotSquaredOldProtectionCompat implements ProtectionCompat
{
    @SuppressWarnings("unused")
    private final BigDoors plugin;
    private final PlotAPI plotSquared;
    private boolean success = false;
    private final JavaPlugin plotSquaredPlugin;
    private PlayerEvents playerEventsListener = null;

    public PlotSquaredOldProtectionCompat(BigDoors plugin)
    {
        this.plugin = plugin;
        plotSquared = new PlotAPI();
        plotSquaredPlugin = JavaPlugin.getPlugin(com.plotsquared.bukkit.BukkitMain.class);

        for (RegisteredListener rl : HandlerList.getRegisteredListeners(plotSquaredPlugin))
            for (Method method : rl.getListener().getClass().getDeclaredMethods())
                if (method.toString().startsWith("public void com.plotsquared.bukkit.listeners.PlayerEvents.blockDestroy"))
                    try
                    {
                        playerEventsListener = (PlayerEvents) rl.getListener();
                        break;
                    }
                    catch (Exception uncaught)
                    {
                        continue;
                    }

        success = playerEventsListener != null;
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        return canBreakBlock(player, plotSquared.getPlot(loc), loc.getWorld());
    }

    private boolean canBreakBlock(Player player, Plot plot, World world)
    {
        if (plot == null)
            return true;
        com.intellectualcrafters.plot.object.Location center = plot.getCenter();

        BlockBreakEvent blockBreakEvent = new BlockBreakEvent(new Location(world, center.getX(), center.getY(), center.getZ()).getBlock(), player);
        playerEventsListener.blockDestroy(blockBreakEvent);
        return !blockBreakEvent.isCancelled();
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (loc1.getWorld() != loc2.getWorld())
            return false;

        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        Plot checkPlot = null;

        for (; x1 <= x2; ++x1)
            for (; z1 <= z2; ++z1)
            {
                Location loc = new Location(loc1.getWorld(), x1, 128, z1);
                Plot newPlot = plotSquared.getPlot(loc);
                if (checkPlot == null || checkPlot != newPlot)
                {
                    checkPlot = newPlot;
                    if (!canBreakBlock(player, checkPlot, loc.getWorld()))
                        return false;
                }
            }
        return true;
    }

    @Override
    public boolean success()
    {
        return success;
    }

    @Override
    public JavaPlugin getPlugin()
    {
        return plotSquaredPlugin;
    }

    @Override
    public String getName()
    {
        return plotSquaredPlugin.getName();
    }
}

