package nl.pim16aap2.animatedarchitecture.compatibility.griefdefender2;

import com.griefdefender.api.Core;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.User;
import com.griefdefender.api.claim.Claim;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
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
    public boolean canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid)
    {
        if (!enabledInWorldChecks(Objects.requireNonNull(world)))
            return true;

        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        for (int xPos = min.x(); xPos <= max.x(); ++xPos)
            for (int yPos = min.y(); yPos <= max.y(); ++yPos)
                for (int zPos = min.z(); zPos <= max.z(); ++zPos)
                    if (!checkLocation(player, new Location(world, xPos, yPos, zPos)))
                        return false;
        return true;
    }

    @Override
    public String getName()
    {
        return "GriefDefender";
    }
}
