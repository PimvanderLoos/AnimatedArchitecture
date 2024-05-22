package nl.pim16aap2.animatedarchitecture.spigot.hooks.towny;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Protection hook for Towny.
 */
@Flogger
public class TownyProtectionHook implements IProtectionHookSpigot
{
    @Getter
    private final ProtectionHookContext context;

    @SuppressWarnings("unused") // Called by reflection.
    public TownyProtectionHook(ProtectionHookContext context)
    {
        this.context = context;
        Objects.requireNonNull(TownyAPI.getInstance());
    }

    private boolean canBreakBlock0(Player player, Location loc)
    {
        final boolean result =
            PlayerCacheUtil.getCachePermission(player, loc, loc.getBlock().getType(), ActionType.DESTROY);
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
    public CompletableFuture<Boolean> canBreakBlocksInCuboid(Player player, World world, Cuboid cuboid)
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
        return context.getSpecification().getName();
    }
}


