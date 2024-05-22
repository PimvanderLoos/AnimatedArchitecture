package nl.pim16aap2.animatedarchitecture.spigot.hooks.plotsquared6;

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
 * Protection hook for PlotSquared 6.
 *
 * @deprecated PlotSquared 6 is no longer supported. It will be removed in a future version.
 */
@Deprecated
@Flogger
public class PlotSquared6ProtectionHook implements IProtectionHookSpigot
{
    private final JavaPlugin plotSquaredPlugin;
    @Getter
    private final ProtectionHookContext context;

    @SuppressWarnings("unused") // Called by reflection.
    public PlotSquared6ProtectionHook(ProtectionHookContext context)
    {
        this.context = context;
        plotSquaredPlugin = JavaPlugin.getPlugin(BukkitPlatform.class);
        log.atSevere().log(
            "PlotSquared 6 support is deprecated and will be removed in a future version. " +
                "Please upgrade to PlotSquared 7.");
    }

    @Override
    public CompletableFuture<Boolean> canBreakBlock(Player player, Location loc)
    {
        final com.plotsquared.core.location.Location psLocation = BukkitUtil.adapt(loc);
        final PlotArea area = psLocation.getPlotArea();

        if (area == null)
            return CompletableFuture.completedFuture(true);

        return CompletableFuture.completedFuture(canBreakBlock(player, area, area.getPlot(psLocation), loc));
    }

    private boolean isHeightAllowed(Player player, PlotArea area, int height)
    {
        if (height == 0)
            return hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_GROUNDLEVEL.toString());
        else
            return (height <= area.getMaxBuildHeight() && height >= area.getMinBuildHeight()) ||
                hasPermission(player, Permission.PERMISSION_ADMIN_BUILD_HEIGHT_LIMIT.toString());
    }

    private boolean canBreakRoads(Player player)
    {
        return hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_ROAD.toString());
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
            return hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_UNOWNED.toString());

        if (!plot.isAdded(player.getUniqueId()))
        {
            final BlockType blockType = BukkitAdapter.asBlockType(loc.getBlock().getType());

            for (final BlockTypeWrapper blockTypeWrapper : plot.getFlag(BreakFlag.class))
                if (blockTypeWrapper.accepts(blockType))
                    return true;

            return hasPermission(player, Permission.PERMISSION_ADMIN_DESTROY_OTHER.toString());
        }
        else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot))
        {
            return hasPermission(player, Permission.PERMISSION_ADMIN_BUILD_OTHER.toString());
        }
        return true;

    }

    @Override
    public CompletableFuture<Boolean> canBreakBlocksInCuboid(Player player, World world, Cuboid cuboid)
    {
        com.plotsquared.core.location.Location psLocation;

        final boolean canBreakRoads = canBreakRoads(player);

        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        for (int xPos = min.x(); xPos <= max.x(); ++xPos)
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
                    return CompletableFuture.completedFuture(false);
                else if (!canBreakBlock(player, area, newPlot, loc))
                    return CompletableFuture.completedFuture(false);
            }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public String getName()
    {
        return plotSquaredPlugin.getName();
    }
}
