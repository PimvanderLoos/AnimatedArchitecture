package nl.pim16aap2.bigDoors.compatibility;

import com.intellectualcrafters.plot.api.PlotAPI;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.flag.Flags;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotArea;
import com.plotsquared.bukkit.BukkitMain;
import com.plotsquared.bukkit.util.BukkitUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Compatibility hook for the old version of PlotSquared.
 *
 * @see IProtectionCompat
 * @author Pim
 */
public class PlotSquared3ProtectionCompat implements IProtectionCompat
{
    private final PlotAPI plotSquared;
    private final JavaPlugin plotSquaredPlugin;
    private final HookContext hookContext;
    private final boolean success;

    public PlotSquared3ProtectionCompat(HookContext hookContext)
    {
        this.hookContext = hookContext;
        plotSquared = new PlotAPI();
        plotSquaredPlugin = JavaPlugin.getPlugin(BukkitMain.class);
        success = plotSquaredPlugin != null;
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        final com.intellectualcrafters.plot.object.Location psLocation = BukkitUtil.getLocation(loc);
        final PlotArea area = psLocation.getPlotArea();

        if (area == null)
            return true;

        return canBreakBlock(player, area, area.getPlot(psLocation), loc);
    }

    private boolean isHeightAllowed(Player player, PlotArea area, int height)
    {
        if (height == 0)
        {
            if (!hookContext.getPermissionsManager().hasPermission(player, C.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL.s()))
                return false;
        }
        else if ((height > area.MAX_BUILD_HEIGHT || height < area.MIN_BUILD_HEIGHT) &&
                 !hookContext.getPermissionsManager().hasPermission(player, C.PERMISSION_ADMIN_BUILD_HEIGHTLIMIT.s()))
            return false;
        return true;
    }

    // Check if a given player is allowed to build in a given plot.
    // Adapted from:
    // https://github.com/IntellectualSites/PlotSquared/blob/e4fbc23d08be268d14c8016ef1d928a2fee9b365/Bukkit/src/main/java/com/plotsquared/bukkit/listeners/PlayerEvents.java#L917
    private boolean canBreakBlock(Player player, PlotArea area, Plot plot, Location loc)
    {
        if (plot != null)
        {
            if (!isHeightAllowed(player, area, loc.getBlockY()))
                return false;

            if (!plot.hasOwner())
                return hookContext.getPermissionsManager().hasPermission(player, C.PERMISSION_ADMIN_DESTROY_UNOWNED.s());

            if (!plot.isAdded(player.getUniqueId()))
            {
                if (hookContext.getPermissionsManager().hasPermission(player, C.PERMISSION_ADMIN_DESTROY_OTHER.s()))
                    return true;
                return false;
            }
            else if (Settings.Done.RESTRICT_BUILDING && plot.getFlags().containsKey(Flags.DONE))
            {
                if (!hookContext.getPermissionsManager().hasPermission(player, C.PERMISSION_ADMIN_BUILD_OTHER.s()))
                    return false;
            }
            return true;
        }

        return canBreakRoads(player);
    }

    private boolean canBreakRoads(Player player)
    {
        return hookContext.getPermissionsManager().hasPermission(player, C.PERMISSION_ADMIN_DESTROY_ROAD.s());
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (!plotSquared.isPlotWorld(loc1.getWorld()))
            return true;

        com.intellectualcrafters.plot.object.Location psLocation;
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        final boolean canBreakRoads = canBreakRoads(player);

        for (int xPos = x1; xPos <= x2; ++xPos)
            for (int zPos = z1; zPos <= z2; ++zPos)
            {
                Location loc = new Location(loc1.getWorld(), xPos, y1, zPos);
                psLocation = BukkitUtil.getLocation(loc);
                PlotArea area = psLocation.getPlotArea();
                if (area == null)
                    continue;
                if (!isHeightAllowed(player, area, y1) || !isHeightAllowed(player, area, y2))
                    return false;
                loc.setY(area.MAX_BUILD_HEIGHT - 1);

                Plot newPlot = area.getPlot(psLocation);
                if (newPlot == null && (!canBreakRoads))
                    return false;
                else if (!canBreakBlock(player, area, newPlot, loc))
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
    public String getName()
    {
        return plotSquaredPlugin.getName();
    }
}
