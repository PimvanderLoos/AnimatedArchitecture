package nl.pim16aap2.bigDoors.compatibility;

import me.angeschossen.lands.api.flags.Flags;
import me.angeschossen.lands.api.integration.LandsIntegration;
import me.angeschossen.lands.api.land.Area;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Compatibility hook for Lands:
 * https://www.spigotmc.org/threads/lands-the-new-way-to-let-players-manage-their-land-create-your-land-today-50-release.304906/
 *
 * @see IProtectionCompat
 * @author Pim
 */
public class LandsProtectionCompat implements IProtectionCompat
{
    private final LandsIntegration landsAddon;
    private final HookContext hookContext;

    public LandsProtectionCompat(HookContext hookContext)
    {
        this.hookContext = hookContext;
        landsAddon = new LandsIntegration(hookContext.getPlugin());
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        final @Nullable Area area = landsAddon.getAreaByLoc(loc);
        if (area == null)
            return true;
        return area.hasFlag(player.getUniqueId(), Flags.BLOCK_BREAK);
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        final UUID playerUUID = player.getUniqueId();
        final World world = loc1.getWorld();

        final int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX()) >> 4;
        final int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        final int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ()) >> 4;
        final int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX()) >> 4;
        final int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        final int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ()) >> 4;

        final Location loc = new Location(world, 0, 0, 0);

        for (int chunkX = x1; chunkX <= x2; ++chunkX)
            for (int chunkZ = z1; chunkZ <= z2; ++chunkZ)
            {
                loc.setX(x1 >> 4);
                loc.setZ(z1 >> 4);
                for (int y = y1; y <= y2; ++y)
                {
                    loc.setY(y);
                    final @Nullable Area area = landsAddon.getAreaByLoc(loc);
                    if (area == null)
                        continue;
                    if (!area.hasFlag(playerUUID, Flags.BLOCK_BREAK))
                        return false;
                }
            }
        return true;
    }

    @Override
    public boolean success()
    {
        return true;
    }

    @Override
    public String getName()
    {
        return hookContext.getProtectionCompatDefinition().getName();
    }
}
