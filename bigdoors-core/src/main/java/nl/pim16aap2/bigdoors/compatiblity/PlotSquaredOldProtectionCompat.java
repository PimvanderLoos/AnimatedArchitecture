package nl.pim16aap2.bigdoors.compatiblity;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.intellectualcrafters.plot.api.PlotAPI;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.flag.Flags;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;

import nl.pim16aap2.bigdoors.BigDoors;

/**
 * Compatibility hook for the old version of PlotSquared.
 *
 * @see ProtectionCompat
 * @author Pim
 */
public class PlotSquaredOldProtectionCompat implements ProtectionCompat
{
    private final BigDoors plugin;
    private final PlotAPI plotSquared;
    private boolean success = false;
    private final JavaPlugin plotSquaredPlugin;

    public PlotSquaredOldProtectionCompat(BigDoors plugin)
    {
        this.plugin = plugin;
        plotSquared = new PlotAPI();
        plotSquaredPlugin = JavaPlugin.getPlugin(com.plotsquared.bukkit.BukkitMain.class);
        success = plotSquaredPlugin != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        com.intellectualcrafters.plot.object.Location psLocation = com.plotsquared.bukkit.util.BukkitUtil.getLocation(loc);
        com.intellectualcrafters.plot.object.PlotArea area = psLocation.getPlotArea();

        if (area == null)
            return true;

        return canBreakBlock(player, area, area.getPlot(psLocation), loc);
    }

    /**
     * {@inheritDoc}
     */
    private boolean isHeightAllowed(Player player, PlotArea area, int height)
    {
        if (height == 0)
        {
            if (!plugin.getVaultManager().hasPermission(player, C.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL.s()))
                return false;
        }
        else if ((height > area.MAX_BUILD_HEIGHT || height < area.MIN_BUILD_HEIGHT) &&
                 !plugin.getVaultManager().hasPermission(player, C.PERMISSION_ADMIN_BUILD_HEIGHTLIMIT.s()))
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    // Check if a given player is allowed to build in a given plot.
    // Adapted from: https://github.com/IntellectualSites/PlotSquared/blob/e4fbc23d08be268d14c8016ef1d928a2fee9b365/Bukkit/src/main/java/com/plotsquared/bukkit/listeners/PlayerEvents.java#L917
    private boolean canBreakBlock(Player player, PlotArea area,
                                  Plot plot, Location loc)
    {
        if (plot != null)
        {
            if (!isHeightAllowed(player, area, loc.getBlockY()))
                return false;

            if (!plot.hasOwner())
                return plugin.getVaultManager().hasPermission(player, C.PERMISSION_ADMIN_DESTROY_UNOWNED.s());

            if (!plot.isAdded(player.getUniqueId()))
            {
                if (plugin.getVaultManager().hasPermission(player, C.PERMISSION_ADMIN_DESTROY_OTHER.s()))
                    return true;
                return false;
            }
            else if (Settings.Done.RESTRICT_BUILDING && plot.getFlags().containsKey(Flags.DONE))
            {
                if (!plugin.getVaultManager().hasPermission(player, C.PERMISSION_ADMIN_BUILD_OTHER.s()))
                    return false;
            }
            return true;
        }

        return plugin.getVaultManager().hasPermission(player, C.PERMISSION_ADMIN_DESTROY_ROAD.s());
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (loc1.getWorld() != loc2.getWorld())
            return false;

        if (!plotSquared.isPlotWorld(loc1.getWorld()))
            return true;

        com.intellectualcrafters.plot.object.Location psLocation = com.plotsquared.bukkit.util.BukkitUtil.getLocation(loc1);
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        Plot checkPlot = null;

        for (int xPos = x1; xPos <= x2; ++xPos)
            for (int zPos = z1; zPos <= z2; ++zPos)
            {
                Location loc = new Location(loc1.getWorld(), xPos, y1, zPos);
                psLocation = com.plotsquared.bukkit.util.BukkitUtil.getLocation(loc);
                PlotArea area = psLocation.getPlotArea();
                if (area == null)
                    continue;
                if (!isHeightAllowed(player,area, y1) || !isHeightAllowed(player,area, y2))
                    return false;
                loc.setY(area.MAX_BUILD_HEIGHT - 1);

                Plot newPlot = area.getPlot(psLocation);
                if (checkPlot == null || !checkPlot.equals(newPlot))
                {
                    checkPlot = newPlot;
                    if (!canBreakBlock(player, area, checkPlot, loc))
                        return false;
                }
            }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean success()
    {
        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JavaPlugin getPlugin()
    {
        return plotSquaredPlugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return getPlugin().getName();
    }
}
