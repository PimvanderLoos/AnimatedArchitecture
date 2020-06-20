package nl.pim16aap2.bigdoors.spigot.compatiblity;

import me.angeschossen.lands.api.integration.LandsIntegration;
import me.angeschossen.lands.api.land.LandChunk;
import me.angeschossen.lands.api.role.enums.RoleSetting;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Compatibility hook for Lands: https://www.spigotmc.org/threads/lands-the-new-way-to-let-players-manage-their-land-create-your-land-today-50-release.304906/
 *
 * @author Pim
 * @see IProtectionCompat
 */
public class LandsProtectionCompat implements IProtectionCompat
{
    private static final ProtectionCompat compat = ProtectionCompat.LANDS;
    private final BigDoorsSpigot plugin;
    private boolean success = false;
    private final LandsIntegration landsAddon;

    public LandsProtectionCompat(final @NotNull BigDoorsSpigot plugin)
    {
        this.plugin = plugin;
        landsAddon = new LandsIntegration(plugin, false);
        success = landsAddon != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBreakBlock(final @NotNull Player player, final @NotNull Location loc)
    {
        return landsAddon.getLandChunk(loc).canAction(player.getUniqueId(), RoleSetting.BLOCK_BREAK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBreakBlocksBetweenLocs(final @NotNull Player player, final @NotNull Location loc1,
                                             final @NotNull Location loc2)
    {
        if (loc1.getWorld() != loc2.getWorld())
            return false;

        UUID playerUUID = player.getUniqueId();
        World world = loc1.getWorld();

        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX()) >> 4;
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ()) >> 4;
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX()) >> 4;
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ()) >> 4;

        for (int chunkX = x1; chunkX <= x2; ++chunkX)
            for (int chunkZ = z1; chunkZ <= z2; ++chunkZ)
            {
                final LandChunk landChunk = landsAddon.getLandChunk(new Location(world, chunkX << 4, 64, chunkZ << 4));
                if (landChunk == null)
                    continue;
                if (!landChunk.canAction(playerUUID, RoleSetting.BLOCK_BREAK))
                    return false;

            }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean success()
    {
        return success;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return ProtectionCompat.getName(compat);
    }
}
