package nl.pim16aap2.animatedarchitecture.spigot.hooks.plotsquared7;

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
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Protection hook for PlotSquared 7.
 */
@Flogger
public class PlotSquared7ProtectionHook implements IProtectionHookSpigot
{
    private final JavaPlugin plotSquaredPlugin;
    @Getter
    private final ProtectionHookContext context;

    @SuppressWarnings("unused") // Called by reflection.
    public PlotSquared7ProtectionHook(ProtectionHookContext context)
    {
        this.context = context;
        plotSquaredPlugin = JavaPlugin.getPlugin(BukkitPlatform.class);
    }

    @Override
    public CompletableFuture<Boolean> canBreakBlock(Player player, Location loc)
    {
        final com.plotsquared.core.location.Location psLocation = BukkitUtil.adapt(loc);
        final @Nullable PlotArea area = psLocation.getPlotArea();

        if (area == null)
            return CompletableFuture.completedFuture(true);

        final boolean result = canBreakBlock(player, area, area.getPlot(psLocation), loc);
        if (!result)
            log.atFine().log(
                "Player %s is not allowed to break block at %s",
                lazyFormatPlayerName(player), loc
            );
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Check if a given height is inside the allowed range for a given plot area.
     *
     * @param height
     *     The height to check.
     * @param area
     *     The plot area whose height range to check.
     * @return True if the given height is inside the allowed range for the given plot area.
     */
    private boolean heightInsideAllowedRange(int height, PlotArea area)
    {
        return height <= area.getMaxBuildHeight() && height > area.getMinBuildHeight();
    }

    /**
     * Check if a given player is allowed to break blocks at a given height in a given plot area.
     *
     * @param player
     *     The player to check.
     * @param area
     *     The plot area to check.
     * @param height
     *     The height to check.
     * @return True if the player is allowed to break blocks at the given height in the given plot area.
     */
    private boolean isHeightAllowed(Player player, PlotArea area, int height)
    {
        final boolean result;
        if (height == 0)
            result = hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL.toString());
        else
            result = heightInsideAllowedRange(height, area) ||
                hasPermission(player, Permission.PERMISSION_ADMIN_BUILD_HEIGHT_LIMIT.toString());

        if (!result)
            log.atFiner().log(
                "Player %s is not allowed to break blocks at height %d in plot area '%s' with region '%s'",
                lazyFormatPlayerName(player), height, area, area.getRegion()
            );
        return result;
    }

    private boolean canBreakRoads(Player player)
    {
        final boolean result = hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_ROAD.toString());
        if (!result)
            log.atFiner().log(
                "Player %s is not allowed to break roads",
                lazyFormatPlayerName(player)
            );
        return result;
    }

    // Check if a given player is allowed to build in a given plot.
    // Adapted from:
    // https://github.com/IntellectualSites/PlotSquared/blob/39d2f1a72c6f69c00319600ebc2c81feef7ecdf8/Bukkit/src/main/java/com/plotsquared/bukkit/listener/BlockEventListener.java#L334
    private boolean canBreakBlock(Player player, PlotArea area, @Nullable Plot plot, Location loc)
    {
        if (plot == null)
            return canBreakRoads(player);

        if (!isHeightAllowed(player, area, loc.getBlockY()))
            return false;

        if (!plot.hasOwner())
        {
            final boolean result = hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_UNOWNED.toString());
            if (!result)
                log.atFiner().log(
                    "Player %s is not allowed to break block in unowned plot '%s' at location '%s'",
                    lazyFormatPlayerName(player), plot, loc
                );
            return result;
        }

        if (!plot.isAdded(player.getUniqueId()))
        {
            final BlockType blockType = BukkitAdapter.asBlockType(loc.getBlock().getType());

            for (final BlockTypeWrapper blockTypeWrapper : plot.getFlag(BreakFlag.class))
                if (blockTypeWrapper.accepts(blockType))
                    return true;

            final boolean result = hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_OTHER.toString());
            if (!result)
                log.atFiner().log(
                    "Player %s is not allowed to break block in plot '%s' at location '%s': " +
                        "block type '%s' is not allowed!",
                    lazyFormatPlayerName(player), plot.toString(), loc, blockType
                );
            return result;
        }
        else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot))
        {
            final boolean result = hasPermission(player, Permission.PERMISSION_ADMIN_BUILD_OTHER.toString());
            if (!result)
                log.atFiner().log(
                    "Player %s is not allowed to break block in plot '%s' at location '%s': plot is marked as done!",
                    lazyFormatPlayerName(player), plot.toString(), loc
                );
            return result;
        }
        else
        {
            return true;
        }
    }

    @Override
    public CompletableFuture<Boolean> canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid)
    {
        com.plotsquared.core.location.Location psLocation;

        final boolean canBreakRoads = canBreakRoads(player);
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        for (int xPos = min.x(); xPos <= max.x(); ++xPos)
        {
            for (int zPos = min.z(); zPos <= max.z(); ++zPos)
            {
                final Location loc = new Location(world, xPos, min.y(), zPos);
                psLocation = BukkitUtil.adapt(loc);
                final PlotArea area = psLocation.getPlotArea();
                if (area == null)
                    continue;

                if (!isHeightAllowed(player, area, min.y()) || !isHeightAllowed(player, area, max.y()))
                    return CompletableFuture.completedFuture(false);

                loc.setY(area.getMaxBuildHeight() - 1);

                final Plot newPlot = area.getPlot(psLocation);
                if (newPlot == null && (!canBreakRoads))
                {
                    log.atFiner().log(
                        "Player %s is not allowed to break block at %s: Not in a plot area and cannot break roads!",
                        lazyFormatPlayerName(player), loc
                    );
                    return CompletableFuture.completedFuture(false);
                }
                else if (!canBreakBlock(player, area, newPlot, loc))
                {
                    return CompletableFuture.completedFuture(false);
                }
            }
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public String getName()
    {
        return plotSquaredPlugin.getName();
    }
}
