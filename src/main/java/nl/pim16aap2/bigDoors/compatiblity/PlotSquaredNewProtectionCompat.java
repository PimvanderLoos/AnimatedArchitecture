package nl.pim16aap2.bigDoors.compatiblity;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.github.intellectualsites.plotsquared.api.PlotAPI;
import com.github.intellectualsites.plotsquared.plot.object.Plot;

import nl.pim16aap2.bigDoors.BigDoors;

public class PlotSquaredNewProtectionCompat implements ProtectionCompat
{
    @SuppressWarnings("unused")
    private final PlotAPI plotSquared;
    @SuppressWarnings("unused")
    private final BigDoors plugin;
    private boolean success = false;
    private final JavaPlugin plotSquaredPlugin;

    public PlotSquaredNewProtectionCompat(BigDoors plugin)
    {
        this.plugin = plugin;
        plotSquared = new PlotAPI();
//        success = true; // This hook isn't implemented yet, so it's not successful.
        plotSquaredPlugin = JavaPlugin.getPlugin(com.github.intellectualsites.plotsquared.bukkit.BukkitMain.class);
    }

    private boolean canBreakBlock(Player player, Plot plot)
    {
        return true;
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        Plot plot = null;
        return canBreakBlock(player, plot);
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (loc1.getWorld() != loc2.getWorld())
            return false;

        int x1 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int z1 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int z2 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());

        for (; x1 <= x2; ++x1)
            for (; z1 <= z2; ++z1)
            {
//                Location loc = new Location(loc1.getWorld(), x1, 128, z1);
//                Set<Plot> x = plotSquared.getPlayerPlots(plotSquared.wrapPlayer(player.getUniqueId()));
//                plotSquared.getPlotSquared().getPlotAreaManager().getPlotArea(loc);

                if (!canBreakBlock(player, new Location(loc1.getWorld(), x1, 128, z1)))
                    return false;
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



