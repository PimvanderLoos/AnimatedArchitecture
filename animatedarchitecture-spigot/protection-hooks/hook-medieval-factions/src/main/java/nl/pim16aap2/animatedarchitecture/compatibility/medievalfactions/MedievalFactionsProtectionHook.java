package nl.pim16aap2.animatedarchitecture.compatibility.medievalfactions;

import com.dansplugins.factionsystem.MedievalFactions;
import com.dansplugins.factionsystem.area.MfBlockPosition;
import com.dansplugins.factionsystem.claim.MfClaimedChunk;
import com.dansplugins.factionsystem.faction.MfFaction;
import com.dansplugins.factionsystem.player.MfPlayer;
import com.dansplugins.factionsystem.service.Services;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;

/**
 * Protection hook for <a href=https://www.spigotmc.org/resources/79941/">Medieval Factions</a>.
 */
public class MedievalFactionsProtectionHook implements IProtectionHookSpigot
{
    private final ProtectionHookContext context;
    private final Services services;

    @SuppressWarnings("unused") // Called by reflection.
    public MedievalFactionsProtectionHook(ProtectionHookContext context)
    {
        this.context = context;
        this.services = JavaPlugin.getPlugin(MedievalFactions.class).getServices();
    }

    /**
     * Checks if a location intersects with a gate in some way. Intersects here is counted as the location either
     * referring to a block that is part of the gate or a to a block that triggers the gate.
     *
     * @param loc
     *     The location to check.
     * @return True if the location intersects with the gate.
     */
    private boolean locationIntersectsWithGate(Location loc)
    {
        final @Nullable MfBlockPosition blockPosition = MfBlockPosition.Companion.fromBukkitLocation(loc);
        if (blockPosition == null)
            return false;
        return services.getGateService().getGatesAt(blockPosition).size() > 0 ||
            services.getGateService().getGatesByTrigger(blockPosition).size() > 0;
    }

    // Adapted from:
    // https://github.com/Dans-Plugins/Medieval-Factions/blob/52b139604da95e3e1d9ca77746d9f50a9de019ad/src/main/kotlin/com/dansplugins/factionsystem/listener/BlockBreakListener.kt#L16
    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        if (locationIntersectsWithGate(loc))
            return false;

        final @Nullable MfClaimedChunk claim = services.getClaimService().getClaim(loc.getChunk());
        if (claim == null)
            return true;

        final @Nullable MfFaction faction = services.getFactionService().getFactionByFactionId(claim.getFactionId());
        if (faction == null)
            return true;

        final @Nullable MfPlayer mfPlayer = services.getPlayerService().getPlayerByBukkitPlayer(player);
        if (mfPlayer == null)
            return false;

        if (services.getClaimService().isInteractionAllowedForPlayerInChunk(mfPlayer.getId(), claim))
            return true;

        return mfPlayer.isBypassEnabled() && player.hasPermission("mf.bypass");
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
    public String getName()
    {
        return context.getSpecification().getName();
    }
}
