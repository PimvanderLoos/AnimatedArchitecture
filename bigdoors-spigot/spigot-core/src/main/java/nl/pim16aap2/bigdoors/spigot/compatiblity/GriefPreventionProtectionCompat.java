package nl.pim16aap2.bigdoors.spigot.compatiblity;

import lombok.NonNull;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;

/**
 * Compatibility hook for GriefPrevention.
 *
 * @author Pim
 * @see IProtectionCompat
 */
class GriefPreventionProtectionCompat implements IProtectionCompat
{
    private static final ProtectionCompat compat = ProtectionCompat.GRIEFPREVENTION;
    private final BigDoorsSpigot plugin;
    private final GriefPrevention griefPrevention;
    private boolean success = false;

    public GriefPreventionProtectionCompat(final @NonNull BigDoorsSpigot plugin)
    {
        this.plugin = plugin;

        Plugin griefPreventionPlugin = Bukkit.getServer().getPluginManager()
                                             .getPlugin(ProtectionCompat.getName(compat));

        // WorldGuard may not be loaded
        if (!(griefPreventionPlugin instanceof GriefPrevention))
        {
            griefPrevention = null;
            return;
        }
        griefPrevention = (GriefPrevention) griefPreventionPlugin;
        success = true;
    }

    @Override
    public boolean canBreakBlock(final @NonNull Player player, final @NonNull Location loc)
    {
        Block block = loc.getBlock();
        BlockBreakEvent blockBreakEvent = new BlockBreakEvent(block, player);
        return griefPrevention.allowBreak(player, block, loc, blockBreakEvent) == null;
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(final @NonNull Player player, final @NonNull Location loc1,
                                             final @NonNull Location loc2)
    {
        if (loc1.getWorld() != loc2.getWorld())
            return false;

        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

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
    public @NonNull String getName()
    {
        return griefPrevention.getName();
    }
}
