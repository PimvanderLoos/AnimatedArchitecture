package nl.pim16aap2.bigDoors.compatibility;

import com.cryptomorin.xseries.XMaterial;
import com.griefdefender.api.Core;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.User;
import com.griefdefender.api.claim.Claim;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class GriefDefender2ProtectionCompat implements IProtectionCompat
{
    private final Core griefDefender = GriefDefender.getCore();

    public GriefDefender2ProtectionCompat(@SuppressWarnings("unused") HookContext hookContext)
    {
    }

    private boolean enabledInWorldChecks(World world)
    {
        return GriefDefender.getCore().isEnabled(world.getUID());
    }

    private boolean isTypeBlockIgnored(Location loc)
    {
        final Block block = loc.getBlock();
        final Material type = block.getType();
        return type.equals(XMaterial.AIR.parseMaterial()) ||
            type.equals(XMaterial.CAVE_AIR.parseMaterial()) ||
            type.equals(XMaterial.VOID_AIR.parseMaterial());
    }

    private boolean checkLocation(Player player, Location loc)
    {
        if (isTypeBlockIgnored(loc))
            return true;

        final Claim targetClaim = griefDefender.getClaimAt(loc);
        if (targetClaim == null || targetClaim.isWilderness())
            return true;

        final User wrappedPlayer = GriefDefender.getCore().getUser(player.getUniqueId());
        return targetClaim.canBreak(player, loc, wrappedPlayer);
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
        return griefDefender != null;
    }

    @Override
    public String getName()
    {
        return "GriefDefender";
    }
}
