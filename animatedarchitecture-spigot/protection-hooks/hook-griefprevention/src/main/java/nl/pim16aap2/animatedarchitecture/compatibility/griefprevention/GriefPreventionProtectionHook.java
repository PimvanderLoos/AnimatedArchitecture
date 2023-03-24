package nl.pim16aap2.animatedarchitecture.compatibility.griefprevention;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.ProtectionHookContext;
import org.bukkit.Location;
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
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        final int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        final int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        final int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        final int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        final int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        final int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        for (int xPos = x1; xPos <= x2; ++xPos)
            for (int yPos = y1; yPos <= y2; ++yPos)
                for (int zPos = z1; zPos <= z2; ++zPos)
                    if (!canBreakBlock(player, new Location(loc1.getWorld(), xPos, yPos, zPos)))
                        return false;
        return true;
    }

    @Override
    public String getName()
    {
        return "GriefPrevention";
    }
}
