package nl.pim16aap2.animatedarchitecture.spigot.hooks.griefdefender2;

import com.griefdefender.api.Core;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.User;
import com.griefdefender.api.claim.Claim;
import lombok.CustomLog;
import lombok.Getter;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.HookPreCheckResult;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

/**
 * Protection hook for GriefDefender 2.
 */
@CustomLog
public class GriefDefender2ProtectionHook implements IProtectionHookSpigot
{
    private final Core griefDefender;
    @Getter
    private final ProtectionHookContext context;

    @SuppressWarnings("unused") // Called by reflection.
    public GriefDefender2ProtectionHook(ProtectionHookContext context)
    {
        griefDefender = GriefDefender.getCore();
        this.context = context;
    }

    private boolean enabledInWorldChecks(World world)
    {
        return GriefDefender.getCore().isEnabled(world.getUID());
    }

    @Override
    public HookPreCheckResult preCheck(Player player, World world)
    {
        return enabledInWorldChecks(world) ? HookPreCheckResult.ALLOW : HookPreCheckResult.BYPASS;
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
        final boolean result = targetClaim.canBreak(player, loc, wrappedPlayer);
        if (!result)
            log.atDebug().log(
                "Player %s is not allowed to break block at %s",
                lazyFormatPlayerName(player),
                loc
            );
        return result;
    }

    @Override
    public CompletableFuture<Boolean> canBreakBlock(Player player, Location loc)
    {
        return CompletableFuture.completedFuture(checkLocation(player, loc));
    }

    @Override
    public CompletableFuture<Boolean> canBreakBlocksInCuboid(Player player, World world, Cuboid cuboid)
    {
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        for (int xPos = min.x(); xPos <= max.x(); ++xPos)
            for (int yPos = min.y(); yPos <= max.y(); ++yPos)
                for (int zPos = min.z(); zPos <= max.z(); ++zPos)
                    if (!checkLocation(player, new Location(world, xPos, yPos, zPos)))
                        return CompletableFuture.completedFuture(false);
        return CompletableFuture.completedFuture(true);
    }
}
