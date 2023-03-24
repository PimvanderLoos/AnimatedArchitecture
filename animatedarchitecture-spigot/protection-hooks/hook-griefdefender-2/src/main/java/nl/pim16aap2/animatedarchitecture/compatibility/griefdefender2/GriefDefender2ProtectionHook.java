package nl.pim16aap2.animatedarchitecture.compatibility.griefdefender2;

import com.griefdefender.api.Core;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.User;
import com.griefdefender.api.claim.Claim;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Protection hook for GriefDefender 2.
 */
public class GriefDefender2ProtectionHook implements IProtectionHookSpigot
{
    private final Core griefDefender;

    @SuppressWarnings("unused") // Called by reflection.
    public GriefDefender2ProtectionHook(ProtectionHookContext context)
    {
        griefDefender = GriefDefender.getCore();
    }

    private boolean enabledInWorldChecks(World world)
    {
        return GriefDefender.getCore().isEnabled(world.getUID());
    }

    private boolean isTypeBlockIgnored(Location loc)
    {
        return loc.getBlock().getType().isAir();
    }

    private boolean checkLocation(Player player, Location loc)
    {
        if (isTypeBlockIgnored(loc))
            return true;

        final Claim targetClaim = griefDefender.getClaimAt(loc);
        if (targetClaim == null || targetClaim.isWilderness())
            return true;

        final User wrappedPlayer = GriefDefender.getCore().getUser(player.getUniqueId());
        return targetClaim.canBreak(player, loc, wrappedPlayer);
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        if (!enabledInWorldChecks(Objects.requireNonNull(loc.getWorld())))
            return true;
        return checkLocation(player, loc);
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (!enabledInWorldChecks(Objects.requireNonNull(loc1.getWorld())))
            return true;

        final int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        final int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        final int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        final int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        final int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        final int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        for (int xPos = x1; xPos <= x2; ++xPos)
            for (int yPos = y1; yPos <= y2; ++yPos)
                for (int zPos = z1; zPos <= z2; ++zPos)
                    if (!checkLocation(player, new Location(loc1.getWorld(), xPos, yPos, zPos)))
                        return false;
        return true;
    }

    @Override
    public String getName()
    {
        return "GriefDefender";
    }
}
