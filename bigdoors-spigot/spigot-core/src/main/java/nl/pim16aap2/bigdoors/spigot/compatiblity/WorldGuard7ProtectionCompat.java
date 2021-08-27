package nl.pim16aap2.bigdoors.spigot.compatiblity;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Compatibility hook for version 7 of WorldGuard.
 *
 * @author Pim
 * @see IProtectionCompat
 */
class WorldGuard7ProtectionCompat implements IProtectionCompat
{
    private static final ProtectionCompat compat = ProtectionCompat.WORLDGUARD;
    private final WorldGuard worldGuard;
    private final WorldGuardPlugin worldGuardPlugin;
    private final boolean success;

    public WorldGuard7ProtectionCompat()
    {
        worldGuard = WorldGuard.getInstance();

        final @Nullable Plugin wgPlugin =
            Bukkit.getServer().getPluginManager().getPlugin(ProtectionCompat.getName(compat));

        // WorldGuard may not be loaded
        if (!(wgPlugin instanceof WorldGuardPlugin))
            throw new IllegalStateException("Plugin " + wgPlugin + " is not the expected WorldGuardPlugin!");

        worldGuardPlugin = (WorldGuardPlugin) wgPlugin;
        success = true;
    }

    private boolean canBreakBlock(LocalPlayer player, Location loc)
    {
        return worldGuard.getPlatform().getRegionContainer().createQuery()
                         .testState(BukkitAdapter.adapt(loc), player,
                                    com.sk89q.worldguard.protection.flags.Flags.BUILD);
    }

    private LocalPlayer getLocalPlayer(Player player)
    {
        return worldGuardPlugin.wrapPlayer(player);
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        return canBreakBlock(getLocalPlayer(player), loc);
    }

    @SuppressWarnings("DuplicatedCode") // This class will need to be rewritten anyway.
    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        if (loc1.getWorld() != loc2.getWorld())
            return false;

        final int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        final int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        final int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        final int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        final int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        final int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        final LocalPlayer lPlayer = getLocalPlayer(player);
        final RegionQuery query = worldGuard.getPlatform().getRegionContainer().createQuery();
        final com.sk89q.worldedit.world.World wgWorld = BukkitAdapter.adapt(Objects.requireNonNull(loc1.getWorld()));

        for (int xPos = x1; xPos <= x2; ++xPos)
            for (int yPos = y1; yPos <= y2; ++yPos)
                for (int zPos = z1; zPos <= z2; ++zPos)
                {
                    final com.sk89q.worldedit.util.Location wgLoc =
                        new com.sk89q.worldedit.util.Location(wgWorld, xPos, yPos, zPos);
                    if (!query.testState(wgLoc, lPlayer, Flags.BUILD))
                        return false;
                }
        return true;
    }

    @Override
    public boolean success()
    {
        return success;
    }

    @Override
    public String getName()
    {
        return worldGuardPlugin.getName();
    }
}
