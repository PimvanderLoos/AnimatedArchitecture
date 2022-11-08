package nl.pim16aap2.bigDoors.compatibility;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.utils.InteractionAccessChecker;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Compatibility hook for Medieval Factions:
 * https://www.spigotmc.org/resources/medieval-factions.79941/
 *
 * @see IProtectionCompat
 * @author Pim
 */
public class MedievalFactionsProtectionCompat implements IProtectionCompat
{
    private final HookContext hookContext;
    private final MedievalFactions medievalFactions;

    public MedievalFactionsProtectionCompat(HookContext hookContext)
    {
        this.hookContext = hookContext;
        this.medievalFactions = MedievalFactions.getInstance();
    }

    // Adapted from:
    // https://github.com/Dans-Plugins/Medieval-Factions/blob/dcf8ecbad2f1f032750a47cffb9f6185fd9641ee/src/main/java/dansplugins/factionsystem/eventhandlers/InteractionHandler.java#L53
    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        final PersistentData data = PersistentData.getInstance();
        final ClaimedChunk chunk = data.getChunkDataAccessor().getClaimedChunk(loc.getChunk());

        if (InteractionAccessChecker.getInstance().shouldEventBeCancelled(chunk, player))
            return false;

        final Block block = loc.getBlock();
        if (data.isBlockInGate(block, player))
            return false;

        return !data.isBlockLocked(block) || data.getLockedBlock(block).getOwner().equals(player.getUniqueId());
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        final World world = loc1.getWorld();

        final int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        final int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        final int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        final int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        final int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        final int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        final Location loc = new Location(world, 0, 0, 0);

        for (int x = x1; x <= x2; ++x)
        {
            loc.setX(x);
            for (int z = z1; z <= z2; ++z)
            {
                loc.setZ(z);
                for (int y = y1; y <= y2; ++y)
                {
                    loc.setY(y);
                    if (!canBreakBlock(player, loc))
                        return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean success()
    {
        return medievalFactions != null;
    }

    @Override
    public String getName()
    {
        return hookContext.getProtectionCompatDefinition().getName();
    }
}
