package nl.pim16aap2.bigDoors.compatibility;

import com.cryptomorin.xseries.XMaterial;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.flag.GDFlags;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

// Adapted from:
// https://github.com/bloodmc/GriefDefender/blob/e281231d874d78def944e748f6b49057a516b3db/bukkit/src/main/java/com/griefdefender/listener/BlockEventHandler.java#L533
public class GriefDefender1ProtectionCompat implements IProtectionCompat
{
    private final GriefDefenderPlugin griefDefenderPlugin = GriefDefenderPlugin.getInstance();

    public GriefDefender1ProtectionCompat(@SuppressWarnings("unused") HookContext hookContext)
    {
    }

    private boolean enabledInWorldChecks(World world)
    {
        if (!GDFlags.BLOCK_BREAK)
            return false;
        return griefDefenderPlugin.claimsEnabledForWorld(world.getUID());
    }

    private boolean isTypeBlockIgnored(Location loc)
    {
        final Block block = loc.getBlock();
        final Material type = block.getType();
        if (type.equals(XMaterial.AIR.parseMaterial()) || type.equals(XMaterial.CAVE_AIR.parseMaterial()) ||
            type.equals(XMaterial.VOID_AIR.parseMaterial()))
            return true;
        return GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_BREAK.getName(), block, loc.getWorld().getUID());
    }

    private boolean checkLocation(Player player, Location loc)
    {
        if (isTypeBlockIgnored(loc))
            return true;
        final GDClaim targetClaim = griefDefenderPlugin.dataStore.getClaimAt(loc);
        final Tristate result = GDPermissionManager.getInstance()
                                                   .getFinalPermission(null, loc, targetClaim, Flags.BLOCK_BREAK,
                                                                       player, loc.getBlock(), player,
                                                                       TrustTypes.BUILDER, true);
        return result != Tristate.FALSE;
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        if (!enabledInWorldChecks(loc.getWorld()))
            return true;
        return checkLocation(player, loc);
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (!enabledInWorldChecks(loc1.getWorld()))
            return true;

        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        for (int xPos = x1; xPos <= x2; ++xPos)
            for (int yPos = y1; yPos <= y2; ++yPos)
                for (int zPos = z1; zPos <= z2; ++zPos)
                    if (!checkLocation(player, new Location(loc1.getWorld(), xPos, yPos, zPos)))
                        return false;
        return true;
    }

    @Override
    public boolean success()
    {
        return griefDefenderPlugin != null;
    }

    @Override
    public String getName()
    {
        return GriefDefenderPlugin.MOD_ID;
    }
}
