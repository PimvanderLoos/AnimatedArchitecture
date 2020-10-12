package nl.pim16aap2.bigDoors.compatiblity;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BlockType;

import nl.pim16aap2.bigDoors.BigDoors;

/**
 * Compatibility hook for the new version of PlotSquared.
 *
 * @see IProtectionCompat
 * @author Pim
 */
public class PlotSquared5ProtectionCompat implements IProtectionCompat
{
    private final BigDoors plugin;
    private boolean success = false;
    private final JavaPlugin plotSquaredPlugin;
    @SuppressWarnings("unused")
    private static final ProtectionCompat compat = ProtectionCompat.PLOTSQUARED;

    public PlotSquared5ProtectionCompat(BigDoors plugin)
    {
        this.plugin = plugin;
        plotSquaredPlugin = JavaPlugin.getPlugin(com.plotsquared.bukkit.BukkitMain.class);
        success = plotSquaredPlugin != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        com.plotsquared.core.location.Location psLocation = com.plotsquared.bukkit.util.BukkitUtil.getLocation(loc);
        com.plotsquared.core.plot.PlotArea area = psLocation.getPlotArea();

        if (area == null)
            return true;

        return canBreakBlock(player, area, area.getPlot(psLocation), loc);
    }

    private boolean isHeightAllowed(Player player, com.plotsquared.core.plot.PlotArea area, int height)
    {
        if (height == 0)
        {
            if (!plugin.getVaultManager()
                .hasPermission(player, com.plotsquared.core.configuration.Captions.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL
                    .getTranslated()))
                return false;
        }
        else if ((height > area.getMaxBuildHeight() || height < area.getMinBuildHeight()) && !plugin.getVaultManager()
            .hasPermission(player, com.plotsquared.core.configuration.Captions.PERMISSION_ADMIN_BUILD_HEIGHT_LIMIT
                .getTranslated()))
            return false;
        return true;
    }

    private boolean canBreakRoads(Player player)
    {
        return plugin.getVaultManager()
            .hasPermission(player,
                           com.plotsquared.core.configuration.Captions.PERMISSION_ADMIN_DESTROY_ROAD.getTranslated());
    }

    // Check if a given player is allowed to build in a given plot.
    // Adapted from:
    // https://github.com/IntellectualSites/PlotSquared/blob/breaking/Bukkit/src/main/java/com/github/intellectualsites/plotsquared/bukkit/listeners/PlayerEvents.java#L981
    private boolean canBreakBlock(Player player, com.plotsquared.core.plot.PlotArea area,
                                  com.plotsquared.core.plot.Plot plot, Location loc)
    {
        if (plot != null)
        {
            if (!isHeightAllowed(player, area, loc.getBlockY()))
                return false;

            if (!plot.hasOwner())
                return plugin.getVaultManager()
                    .hasPermission(player, com.plotsquared.core.configuration.Captions.PERMISSION_ADMIN_DESTROY_UNOWNED
                        .getTranslated());

            if (!plot.isAdded(player.getUniqueId()))
            {
                BlockType blockType = BukkitAdapter.asBlockType(loc.getBlock().getType());
                for (final com.plotsquared.core.plot.flag.types.BlockTypeWrapper blockTypeWrapper : plot
                    .getFlag(com.plotsquared.core.plot.flag.implementations.BreakFlag.class))
                    if (blockTypeWrapper.accepts(blockType))
                        return true;

                if (plugin.getVaultManager()
                    .hasPermission(player, com.plotsquared.core.configuration.Captions.PERMISSION_ADMIN_DESTROY_OTHER
                        .getTranslated()))
                    return true;
                return false;
            }
            else if (com.plotsquared.core.configuration.Settings.Done.RESTRICT_BUILDING &&
                     com.plotsquared.core.plot.flag.implementations.DoneFlag.isDone(plot))
            {
                if (!plugin.getVaultManager()
                    .hasPermission(player, com.plotsquared.core.configuration.Captions.PERMISSION_ADMIN_BUILD_OTHER
                        .getTranslated()))
                    return false;
            }
            return true;
        }

        return canBreakRoads(player);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        com.plotsquared.core.location.Location psLocation = com.plotsquared.bukkit.util.BukkitUtil.getLocation(loc1);
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
                psLocation = com.plotsquared.bukkit.util.BukkitUtil.getLocation(loc);
                com.plotsquared.core.plot.PlotArea area = psLocation.getPlotArea();
                if (area == null)
                    continue;

                if (!isHeightAllowed(player, area, y1) || !isHeightAllowed(player, area, y2))
                    return false;

                loc.setY(area.getMaxBuildHeight() - 1);

                com.plotsquared.core.plot.Plot newPlot = area.getPlot(psLocation);

                if (newPlot == null && (!canBreakRoads))
                    return false;
                else if (!canBreakBlock(player, area, newPlot, loc))
                    return false;
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
    public String getName()
    {
        return plotSquaredPlugin.getName();
    }
}
