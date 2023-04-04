package nl.pim16aap2.animatedarchitecture.spigot.hooks.griefprevention;

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

/**
 * Protection hook for GriefPrevention.
 */
public class GriefPreventionProtectionHook implements IProtectionHookSpigot
{
    private final @Nullable GriefPrevention griefPrevention;

    @SuppressWarnings("unused")
    public GriefPreventionProtectionHook(ProtectionHookContext context)
    {
        griefPrevention = JavaPlugin.getPlugin(GriefPrevention.class);
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        if (griefPrevention == null)
            return false;

        final Block block = loc.getBlock();
        final BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
        return griefPrevention.allowBreak(player, block, loc, blockBreakEvent) == null;
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid)
    {
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        for (int xPos = min.x(); xPos <= max.x(); ++xPos)
            for (int yPos = min.y(); yPos <= max.y(); ++yPos)
                for (int zPos = min.z(); zPos <= max.z(); ++zPos)
                    if (!canBreakBlock(player, new Location(world, xPos, yPos, zPos)))
                        return false;
        return true;
    }

    @Override
    public String getName()
    {
        return "GriefPrevention";
    }
}
