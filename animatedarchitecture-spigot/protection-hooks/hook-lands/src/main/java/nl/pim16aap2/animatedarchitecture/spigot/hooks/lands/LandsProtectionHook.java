package nl.pim16aap2.animatedarchitecture.spigot.hooks.lands;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.land.Area;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Protection hook for <a href="https://www.spigotmc.org/resources/53313/">Lands</a>.
 * <p>
 * Uses <a href="https://github.com/Angeschossen/LandsAPI">LandsAPI</a>.
 */
@Flogger
public class LandsProtectionHook implements IProtectionHookSpigot
{
    private final LandsIntegration landsAddon;
    @Getter
    private final ProtectionHookContext context;

    @SuppressWarnings("unused") // Called by reflection.
    public LandsProtectionHook(ProtectionHookContext context)
    {
        this.context = context;
        landsAddon = LandsIntegration.of(context.getPlugin());
    }

    private boolean canBreakBlock(@Nullable Area area, Player player, Location loc)
    {
        if (area == null)
            return true;

        final boolean result = area.hasRoleFlag(player.getUniqueId(), Flags.BLOCK_BREAK);
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
        return CompletableFuture.completedFuture(canBreakBlock(landsAddon.getArea(loc), player, loc));
    }

    @Override
    public CompletableFuture<Boolean> canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid)
    {
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        final int x1 = min.x() >> 4;
        final int z1 = min.z() >> 4;
        final int x2 = max.x() >> 4;
        final int z2 = max.z() >> 4;

        final Location loc = new Location(world, 0, 0, 0);

        for (int chunkX = x1; chunkX <= x2; ++chunkX)
        {
            loc.setX(chunkX << 4);
            for (int chunkZ = z1; chunkZ <= z2; ++chunkZ)
            {
                loc.setZ(chunkZ << 4);
                for (int y = min.y(); y <= max.y(); ++y)
                {
                    loc.setY(y);

                    final @Nullable Area area = landsAddon.getArea(loc);
                    if (area == null)
                        continue;

                    if (!canBreakBlock(area, player, loc))
                        return CompletableFuture.completedFuture(false);
                }
            }
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public String getName()
    {
        return context.getSpecification().getName();
    }
}
