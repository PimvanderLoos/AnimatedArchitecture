package nl.pim16aap2.bigdoors.spigot.compatiblity;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * Compatibility hook for GriefPrevention.
 *
 * @author Pim
 * @see IProtectionCompat
 */
class GriefPreventionProtectionCompat implements IProtectionCompat
{
    private static final ProtectionCompat COMPAT = ProtectionCompat.GRIEFPREVENTION;
    private final GriefPrevention griefPrevention;
    private final boolean success;

    @SuppressWarnings("unused")
    public GriefPreventionProtectionCompat(JavaPlugin bigDoors, IPLogger logger)
    {
        final @Nullable Plugin griefPreventionPlugin = Bukkit.getServer().getPluginManager()
                                                             .getPlugin(ProtectionCompat.getName(COMPAT));

        // WorldGuard may not be loaded
        if (!(griefPreventionPlugin instanceof GriefPrevention))
            throw new IllegalStateException(
                "Plugin " + griefPreventionPlugin + " is not the expected GriefPrevention!");

        griefPrevention = (GriefPrevention) griefPreventionPlugin;
        success = true;
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        final Block block = loc.getBlock();
        final BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
        return griefPrevention.allowBreak(player, block, loc, blockBreakEvent) == null;
    }

    @SuppressWarnings("DuplicatedCode") // This class will need to be rewritten anyway.
    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (loc1.getWorld() != loc2.getWorld())
            return false;

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
    public boolean success()
    {
        return success;
    }

    @Override
    public String getName()
    {
        return griefPrevention.getName();
    }
}
