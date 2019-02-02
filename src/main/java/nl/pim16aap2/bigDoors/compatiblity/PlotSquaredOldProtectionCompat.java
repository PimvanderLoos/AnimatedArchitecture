package nl.pim16aap2.bigDoors.compatiblity;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.intellectualcrafters.plot.api.PlotAPI;
import com.intellectualcrafters.plot.object.Plot;

import nl.pim16aap2.bigDoors.BigDoors;

public class PlotSquaredOldProtectionCompat implements ProtectionCompat
{
    private final PlotAPI plotSquared;
    @SuppressWarnings("unused")
    private final BigDoors plugin;
    private boolean success = false;
    private final JavaPlugin plotSquaredPlugin;

    public PlotSquaredOldProtectionCompat(BigDoors plugin)
    {
        this.plugin = plugin;
        plotSquared = new PlotAPI();
        success = true;
        plotSquaredPlugin = JavaPlugin.getPlugin(com.plotsquared.bukkit.BukkitMain.class);
    }

    private boolean canBreakBlock(Player player, Plot plot, World world)
    {
        // No plot, no restriction
        if (plot == null || player == null)
            return true;
        return plotSquared.getPlayerPlots(world, player).contains(plot);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        if (!plotSquared.isPlotWorld(loc.getWorld()))
            return true;
        return canBreakBlock(player, plotSquared.getPlot(loc), loc.getWorld());
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (loc1.getWorld() != loc2.getWorld())
            return false;
        if (!plotSquared.isPlotWorld(loc1.getWorld()))
            return true;

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
}

