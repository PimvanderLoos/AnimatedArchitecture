package nl.pim16aap2.bigdoors.compatiblity;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import nl.pim16aap2.bigdoors.BigDoors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Compatibility hook for version 7 of WorldGuard.
 *
 * @author Pim
 * @see IProtectionCompat
 */
class WorldGuard7ProtectionCompat implements IProtectionCompat
{
    private static final ProtectionCompat compat = ProtectionCompat.WORLDGUARD;
    @SuppressWarnings("unused")
    private final BigDoors plugin;
    private final WorldGuard worldGuard;
    private final WorldGuardPlugin worldGuardPlugin;
    private boolean success = false;

    public WorldGuard7ProtectionCompat(BigDoors plugin)
    {
        this.plugin = plugin;
        worldGuard = WorldGuard.getInstance();

        Plugin wgPlugin = Bukkit.getServer().getPluginManager().getPlugin(ProtectionCompat.getName(compat));

        // WorldGuard may not be loaded
        if (plugin == null || !(wgPlugin instanceof WorldGuardPlugin))
        {
            worldGuardPlugin = null;
            return;
        }
        worldGuardPlugin = (WorldGuardPlugin) wgPlugin;
        success = true;
    }

    private boolean canBreakBlock(LocalPlayer player, Location loc)
    {
        return worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(loc), player,
                                                                                     com.sk89q.worldguard.protection.flags.Flags.BUILD);
    }

    private LocalPlayer getLocalPlayer(Player player)
    {
        return worldGuardPlugin.wrapPlayer(player);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBreakBlock(@NotNull Player player, @NotNull Location loc)
    {
        return canBreakBlock(getLocalPlayer(player), loc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBreakBlocksBetweenLocs(@NotNull Player player, @NotNull Location loc1, @NotNull Location loc2)
    {
        if (loc1.getWorld() != loc2.getWorld())
            return false;

        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        LocalPlayer lPlayer = getLocalPlayer(player);
        RegionQuery query = worldGuard.getPlatform().getRegionContainer().createQuery();
        com.sk89q.worldedit.world.World wgWorld = BukkitAdapter.adapt(loc1.getWorld());

        for (int xPos = x1; xPos <= x2; ++xPos)
            for (int yPos = y1; yPos <= y2; ++yPos)
                for (int zPos = z1; zPos <= z2; ++zPos)
                {
                    com.sk89q.worldedit.util.Location wgLoc = new com.sk89q.worldedit.util.Location(wgWorld, xPos, yPos,
                                                                                                    zPos);
                    if (!query.testState(wgLoc, lPlayer, Flags.BUILD))
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
    @NotNull
    @Override
    public JavaPlugin getPlugin()
    {
        return worldGuardPlugin;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public String getName()
    {
        return worldGuardPlugin.getName();
    }
}
