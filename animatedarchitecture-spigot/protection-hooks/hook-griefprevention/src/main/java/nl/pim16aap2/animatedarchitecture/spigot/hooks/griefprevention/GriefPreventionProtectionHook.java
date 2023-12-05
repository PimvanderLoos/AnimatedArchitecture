package nl.pim16aap2.animatedarchitecture.spigot.hooks.griefprevention;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Protection hook for GriefPrevention.
 */
@Flogger
public class GriefPreventionProtectionHook implements IProtectionHookSpigot
{
    private final @Nullable GriefPrevention griefPrevention;
    @Getter
    private final ProtectionHookContext context;

    @SuppressWarnings("unused")
    public GriefPreventionProtectionHook(ProtectionHookContext context)
    {
        griefPrevention = JavaPlugin.getPlugin(GriefPrevention.class);
        this.context = context;
    }

    private boolean canBreakBlock0(Player player, Location loc)
    {
        if (griefPrevention == null)
        {
            log.atInfo().log(
                "Failed to access GriefPrevention while checking if player %s can break block at %s; " +
                    "defaulting to false.",
                lazyFormatPlayerName(player), loc
            );
            return false;
        }

        final Block block = loc.getBlock();
        final BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
        final boolean result = griefPrevention.allowBreak(player, block, loc, blockBreakEvent) == null;
        if (!result)
            log.atFine().log(
                "Player %s is not allowed to break block at %s",
                lazyFormatPlayerName(player), loc
            );
        return result;
    }

    @Override
    public CompletableFuture<Boolean> canBreakBlock(Player player, Location loc)
    {
        return CompletableFuture.completedFuture(canBreakBlock0(player, loc));
    }

    @Override
    public CompletableFuture<Boolean> canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid)
    {
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        for (int xPos = min.x(); xPos <= max.x(); ++xPos)
            for (int yPos = min.y(); yPos <= max.y(); ++yPos)
                for (int zPos = min.z(); zPos <= max.z(); ++zPos)
                    if (!canBreakBlock0(player, new Location(world, xPos, yPos, zPos)))
                        return CompletableFuture.completedFuture(false);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public String getName()
    {
        return "GriefPrevention";
    }
}
