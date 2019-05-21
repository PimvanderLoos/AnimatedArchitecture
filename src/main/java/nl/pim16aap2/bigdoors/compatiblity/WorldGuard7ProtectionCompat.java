package nl.pim16aap2.bigdoors.compatiblity;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import nl.pim16aap2.bigdoors.BigDoors;

public class WorldGuard7ProtectionCompat implements ProtectionCompat
{
    @SuppressWarnings("unused")
    private final BigDoors plugin;
    private final WorldGuard worldGuard;
    private final WorldGuardPlugin worldGuardPlugin;
    private boolean success = false;

    public WorldGuard7ProtectionCompat(BigDoors plugin)
    {
        this.plugin = plugin;
        worldGuard = WorldGuard.getInstance();

        Plugin wgPlugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");

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
        return worldGuard.getPlatform().getRegionContainer().createQuery().testState(BukkitAdapter.adapt(loc), player, com.sk89q.worldguard.protection.flags.Flags.BUILD);
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

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
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
        RegionQuery query   = worldGuard.getPlatform().getRegionContainer().createQuery();
        com.sk89q.worldedit.world.World wgWorld = BukkitAdapter.adapt(loc1.getWorld());

        for (; x1 <= x2; ++x1)
            for (; y1 <= y2; ++y1)
                for (; z1 <= z2; ++z1)
                {
                    com.sk89q.worldedit.util.Location wgLoc = new com.sk89q.worldedit.util.Location(wgWorld, x1, y1, z1);
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
    public JavaPlugin getPlugin()
    {
        return worldGuardPlugin;
    }

    @Override
    public String getName()
    {
        return worldGuardPlugin.getName();
    }
}