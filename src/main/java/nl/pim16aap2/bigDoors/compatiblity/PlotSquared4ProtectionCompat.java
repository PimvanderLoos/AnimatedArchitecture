package nl.pim16aap2.bigDoors.compatiblity;

import java.util.HashSet;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigDoors.BigDoors;

/**
 * Compatibility hook for the new version of PlotSquared.
 *
 * @see IProtectionCompat
 * @author Pim
 */
public class PlotSquared4ProtectionCompat implements IProtectionCompat
{
    private final BigDoors plugin;
    private boolean success = false;
    private final JavaPlugin plotSquaredPlugin;
    @SuppressWarnings("unused")
    private static final ProtectionCompat compat = ProtectionCompat.PLOTSQUARED;

    public PlotSquared4ProtectionCompat(BigDoors plugin)
    {
        this.plugin = plugin;
        plotSquaredPlugin = JavaPlugin.getPlugin(com.github.intellectualsites.plotsquared.bukkit.BukkitMain.class);
        success = plotSquaredPlugin != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        com.github.intellectualsites.plotsquared.plot.object.Location psLocation = com.github.intellectualsites.plotsquared.bukkit.util.BukkitUtil
            .getLocation(loc);
        com.github.intellectualsites.plotsquared.plot.object.PlotArea area = psLocation.getPlotArea();

        if (area == null)
            return true;

        return canBreakBlock(player, area, area.getPlot(psLocation), loc);
    }

    private boolean isHeightAllowed(Player player, com.github.intellectualsites.plotsquared.plot.object.PlotArea area,
                                    int height)
    {
        if (height == 0)
        {
            if (!plugin.getVaultManager()
                .hasPermission(player,
                               com.github.intellectualsites.plotsquared.plot.config.Captions.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL
                                   .s()))
                return false;
        }
        else if ((height > area.MAX_BUILD_HEIGHT || height < area.MIN_BUILD_HEIGHT) && !plugin.getVaultManager()
            .hasPermission(player,
                           com.github.intellectualsites.plotsquared.plot.config.Captions.PERMISSION_ADMIN_BUILD_HEIGHTLIMIT
                               .s()))
            return false;
        return true;
    }

    // Check if a given player is allowed to build in a given plot.
    // Adapted from:
    // https://github.com/IntellectualSites/PlotSquared/blob/breaking/Bukkit/src/main/java/com/github/intellectualsites/plotsquared/bukkit/listeners/PlayerEvents.java#L981
    private boolean canBreakBlock(Player player, com.github.intellectualsites.plotsquared.plot.object.PlotArea area,
                                  com.github.intellectualsites.plotsquared.plot.object.Plot plot, Location loc)
    {
        if (plot != null)
        {
            if (!isHeightAllowed(player, area, loc.getBlockY()))
                return false;

            if (!plot.hasOwner())
                return plugin.getVaultManager()
                    .hasPermission(player,
                                   com.github.intellectualsites.plotsquared.plot.config.Captions.PERMISSION_ADMIN_DESTROY_UNOWNED
                                       .s());

            if (!plot.isAdded(player.getUniqueId()))
            {
                Optional<HashSet<com.github.intellectualsites.plotsquared.plot.object.PlotBlock>> destroy = plot
                    .getFlag(com.github.intellectualsites.plotsquared.plot.flag.Flags.BREAK);
                Block block = loc.getBlock();
                if (destroy.isPresent() &&
                    destroy.get().contains(com.github.intellectualsites.plotsquared.plot.object.PlotBlock
                        .get(block.getType().name())))
                    return true;

                if (plugin.getVaultManager()
                    .hasPermission(player,
                                   com.github.intellectualsites.plotsquared.plot.config.Captions.PERMISSION_ADMIN_DESTROY_OTHER
                                       .s()))
                    return true;
                return false;
            }
            else if (com.github.intellectualsites.plotsquared.plot.config.Settings.Done.RESTRICT_BUILDING &&
                     plot.getFlags().containsKey(com.github.intellectualsites.plotsquared.plot.flag.Flags.DONE))
            {
                if (!plugin.getVaultManager()
                    .hasPermission(player,
                                   com.github.intellectualsites.plotsquared.plot.config.Captions.PERMISSION_ADMIN_BUILD_OTHER
                                       .s()))
                    return false;
            }
            return true;
        }
        return plugin.getVaultManager()
            .hasPermission(player,
                           com.github.intellectualsites.plotsquared.plot.config.Captions.PERMISSION_ADMIN_DESTROY_ROAD
                               .s());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        com.github.intellectualsites.plotsquared.plot.object.Location psLocation = com.github.intellectualsites.plotsquared.bukkit.util.BukkitUtil
            .getLocation(loc1);
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        for (int xPos = x1; xPos <= x2; ++xPos)
            for (int zPos = z1; zPos <= z2; ++zPos)
            {
                Location loc = new Location(loc1.getWorld(), xPos, y1, zPos);
                psLocation = com.github.intellectualsites.plotsquared.bukkit.util.BukkitUtil.getLocation(loc);
                com.github.intellectualsites.plotsquared.plot.object.PlotArea area = psLocation.getPlotArea();
                if (area == null)
                    continue;

                if (!isHeightAllowed(player, area, y1) || !isHeightAllowed(player, area, y2))
                    return false;

                loc.setY(area.MAX_BUILD_HEIGHT - 1);

                com.github.intellectualsites.plotsquared.plot.object.Plot newPlot = area.getPlot(psLocation);
                if (newPlot != null && !canBreakBlock(player, area, newPlot, loc))
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
