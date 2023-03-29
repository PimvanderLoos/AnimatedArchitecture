package nl.pim16aap2.animatedarchitecture.compatibility.lands;

import me.angeschossen.lands.api.flags.Flags;
import me.angeschossen.lands.api.integration.LandsIntegration;
import me.angeschossen.lands.api.land.Area;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Protection hook for <a href="https://www.spigotmc.org/resources/53313/">Lands</a>.
 */
public class LandsProtectionHook implements IProtectionHookSpigot
{
    private final LandsIntegration landsAddon;
    private final ProtectionHookContext context;

    @SuppressWarnings("unused") // Called by reflection.
    public LandsProtectionHook(ProtectionHookContext context)
    {
        this.context = context;
        landsAddon = new LandsIntegration(context.getPlugin());
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
    public boolean canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid)
    {
        final UUID playerUUID = player.getUniqueId();

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
                    final @Nullable Area area = landsAddon.getAreaByLoc(loc);
                    if (area == null)
                        continue;
                    if (!area.hasFlag(playerUUID, Flags.BLOCK_BREAK))
                        return false;
                }
            }
        }
        return true;
    }

    @Override
    public String getName()
    {
        return context.getSpecification().getName();
    }
}
