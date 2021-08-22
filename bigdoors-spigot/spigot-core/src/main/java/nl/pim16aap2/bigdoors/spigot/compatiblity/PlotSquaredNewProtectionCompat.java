package nl.pim16aap2.bigdoors.spigot.compatiblity;

import com.github.intellectualsites.plotsquared.bukkit.util.BukkitUtil;
import com.github.intellectualsites.plotsquared.plot.config.Captions;
import com.github.intellectualsites.plotsquared.plot.config.Settings;
import com.github.intellectualsites.plotsquared.plot.flag.Flags;
import com.github.intellectualsites.plotsquared.plot.object.Plot;
import com.github.intellectualsites.plotsquared.plot.object.PlotArea;
import com.github.intellectualsites.plotsquared.plot.object.PlotBlock;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;

/**
 * Compatibility hook for the new version of PlotSquared.
 *
 * @author Pim
 * @see IProtectionCompat
 */
public class PlotSquaredNewProtectionCompat implements IProtectionCompat
{
    private final BigDoorsSpigot plugin;
    private final JavaPlugin plotSquaredPlugin;
    private final boolean success;

    public PlotSquaredNewProtectionCompat()
    {
        plugin = BigDoorsSpigot.get();
        plotSquaredPlugin = JavaPlugin.getPlugin(com.github.intellectualsites.plotsquared.bukkit.BukkitMain.class);
        success = true;
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        com.github.intellectualsites.plotsquared.plot.object.Location psLocation = BukkitUtil.getLocation(loc);
        com.github.intellectualsites.plotsquared.plot.object.PlotArea area = psLocation.getPlotArea();

        if (area == null)
            return true;

        return canBreakBlock(player, area, area.getPlot(psLocation), loc);
    }

    private boolean isHeightAllowed(Player player, PlotArea area, int height)
    {
        if (height == 0)
            return plugin.getVaultManager()
                         .hasPermission(player, Captions.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL.getTranslated());

        else return (height <= area.MAX_BUILD_HEIGHT && height >= area.MIN_BUILD_HEIGHT) ||
            plugin.getVaultManager()
                  .hasPermission(player, Captions.PERMISSION_ADMIN_BUILD_HEIGHT_LIMIT.getTranslated());
    }

    // Check if a given player is allowed to build in a given plot.
    // Adapted from:
    // https://github.com/IntellectualSites/PlotSquared/blob/breaking/Bukkit/src/main/java/com/github/intellectualsites/plotsquared/bukkit/listeners/PlayerEvents.java#L981
    private boolean canBreakBlock(Player player, PlotArea area, @Nullable Plot plot, Location loc)
    {
        if (plot != null)
        {
            if (!isHeightAllowed(player, area, loc.getBlockY()))
                return false;

            if (!plot.hasOwner())
                return plugin.getVaultManager()
                             .hasPermission(player, Captions.PERMISSION_ADMIN_DESTROY_UNOWNED.getTranslated());

            if (!plot.isAdded(player.getUniqueId()))
            {
                Optional<HashSet<PlotBlock>> destroy = plot.getFlag(Flags.BREAK);
                Block block = loc.getBlock();
                if (destroy.isPresent() && destroy.get().contains(PlotBlock.get(block.getType().name())))
                    return true;

                return plugin.getVaultManager()
                             .hasPermission(player, Captions.PERMISSION_ADMIN_DESTROY_OTHER.getTranslated());
            }
            else if (Settings.Done.RESTRICT_BUILDING && plot.getFlags().containsKey(Flags.DONE))
                return plugin.getVaultManager()
                             .hasPermission(player, Captions.PERMISSION_ADMIN_BUILD_OTHER.getTranslated());

            return true;
        }
        return plugin.getVaultManager().hasPermission(player, Captions.PERMISSION_ADMIN_DESTROY_ROAD.getTranslated());
    }

    @SuppressWarnings("DuplicatedCode") // This class will need to be rewritten anyway.
    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (loc1.getWorld() != loc2.getWorld())
            return false;

        com.github.intellectualsites.plotsquared.plot.object.Location psLocation;
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        @Nullable Plot checkPlot = null;

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

                loc.setY(area.MAX_BUILD_HEIGHT - 1.0);

                @Nullable Plot newPlot = area.getPlot(psLocation);
                if (checkPlot == null || !checkPlot.equals(newPlot))
                {
                    checkPlot = newPlot;
                    if (!canBreakBlock(player, area, checkPlot, loc))
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
    public String getName()
    {
        return plotSquaredPlugin.getName();
    }
}
