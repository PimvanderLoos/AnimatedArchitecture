package nl.pim16aap2.bigdoors.spigot.compatiblity;

import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Compatibility hook for the new version of PlotSquared.
 *
 * @author Pim
 * @see IProtectionCompat
 */
public class TownyNewProtectionCompat implements IProtectionCompat
{
    private static final ProtectionCompat compat = ProtectionCompat.TOWNY;
    private final boolean success;

    public TownyNewProtectionCompat()
    {
        success = true;
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        return PlayerCacheUtil.getCachePermission(player, loc, loc.getBlock().getType(), ActionType.DESTROY);
    }

    @SuppressWarnings("DuplicatedCode") // This class will need to be rewritten anyway.
    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
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
    public String getName()
    {
        return ProtectionCompat.getName(compat);
    }
}


