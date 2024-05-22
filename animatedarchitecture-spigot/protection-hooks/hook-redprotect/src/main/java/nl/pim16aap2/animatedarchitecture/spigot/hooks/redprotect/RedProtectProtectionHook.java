package nl.pim16aap2.animatedarchitecture.spigot.hooks.redprotect;

import br.net.fabiozumbi12.RedProtect.Bukkit.API.RedProtectAPI;
import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * Protection hook for RedProtect.
 */
@Flogger
public class RedProtectProtectionHook implements IProtectionHookSpigot
{
    private final RedProtect redProtect;
    @Getter
    private final ProtectionHookContext context;

    @SuppressWarnings("unused") // Called by reflection.
    public RedProtectProtectionHook(ProtectionHookContext context)
    {
        this.context = context;
        this.redProtect = JavaPlugin.getPlugin(RedProtect.class);
    }

    private boolean canBreakBlock0(Player player, Location loc)
    {
        try
        {
            final RedProtectAPI rpAPI = redProtect.getAPI();
            final Region rpRegion = rpAPI.getRegion(loc);
            final boolean result = rpRegion == null || rpRegion.canBuild(player);
            if (!result)
                log.atFine().log(
                    "Player %s is not allowed to break block at %s",
                    lazyFormatPlayerName(player), loc
                );
            return result;
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log(
                "Failed to check if player %s can break block at %s; defaulting to false.",
                lazyFormatPlayerName(player), loc
            );
            return false;
        }
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
