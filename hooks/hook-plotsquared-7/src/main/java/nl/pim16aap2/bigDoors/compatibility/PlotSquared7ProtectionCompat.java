package nl.pim16aap2.bigDoors.compatibility;

import com.plotsquared.bukkit.BukkitPlatform;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.implementations.BreakFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.plot.flag.types.BlockTypeWrapper;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Compatibility hook for the new version of PlotSquared.
 *
 * @author Pim
 * @see IProtectionCompat
 */
public class PlotSquared7ProtectionCompat implements IProtectionCompat
{
    private final JavaPlugin plotSquaredPlugin;
    private final HookContext hookContext;
    private final boolean success;

    public PlotSquared7ProtectionCompat(HookContext hookContext)
    {
        this.hookContext = hookContext;
        plotSquaredPlugin = JavaPlugin.getPlugin(BukkitPlatform.class);
        success = plotSquaredPlugin != null;
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        final com.plotsquared.core.location.Location psLocation = BukkitUtil.adapt(loc);
        final PlotArea area = psLocation.getPlotArea();

        if (area == null)
            return true;

        return canBreakBlock(player, area, area.getPlot(psLocation), loc);
    }

    private boolean isHeightAllowed(Player player, PlotArea area, int height)
    {
        if (height == 0)
        {
            return hookContext.getPermissionsManager()
                              .hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL.toString());
        }
        else return (height <= area.getMaxBuildHeight() && height >= area.getMinBuildHeight()) ||
            hookContext.getPermissionsManager()
                       .hasPermission(player, Permission.PERMISSION_ADMIN_BUILD_HEIGHT_LIMIT.toString());
    }

    private boolean canBreakRoads(Player player)
    {
        return hookContext.getPermissionsManager()
                          .hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_ROAD.toString());
    }

    // Check if a given player is allowed to build in a given plot.
    // Adapted from:
    // https://github.com/IntellectualSites/PlotSquared/blob/39d2f1a72c6f69c00319600ebc2c81feef7ecdf8/Bukkit/src/main/java/com/plotsquared/bukkit/listener/BlockEventListener.java#L334
    private boolean canBreakBlock(Player player, PlotArea area, Plot plot, Location loc)
    {
        if (plot != null)
        {
            if (!isHeightAllowed(player, area, loc.getBlockY()))
                return false;

            if (!plot.hasOwner())
                return hookContext.getPermissionsManager()
                                  .hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_UNOWNED.toString());

            if (!plot.isAdded(player.getUniqueId()))
            {
                final BlockType blockType = BukkitAdapter.asBlockType(loc.getBlock().getType());

                for (final BlockTypeWrapper blockTypeWrapper : plot.getFlag(BreakFlag.class))
                    if (blockTypeWrapper.accepts(blockType))
                        return true;

                return hookContext.getPermissionsManager()
                                  .hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_OTHER.toString());
            }
            else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot))
            {
                return hookContext.getPermissionsManager()
                                  .hasPermission(player, Permission.PERMISSION_ADMIN_BUILD_OTHER.toString());
            }
            return true;
        }

        return canBreakRoads(player);
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        com.plotsquared.core.location.Location psLocation;
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
                final Location loc = new Location(loc1.getWorld(), xPos, y1, zPos);
                psLocation = BukkitUtil.adapt(loc);
                final PlotArea area = psLocation.getPlotArea();
                if (area == null)
                    continue;

                if (!isHeightAllowed(player, area, y1) || !isHeightAllowed(player, area, y2))
                    return false;

                loc.setY(area.getMaxBuildHeight() - 1);

                final Plot newPlot = area.getPlot(psLocation);

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
