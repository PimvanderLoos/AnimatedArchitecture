package nl.pim16aap2.bigdoors.compatiblity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import nl.pim16aap2.bigdoors.BigDoors;

public class WorldGuard6ProtectionCompat implements ProtectionCompat
{
    @SuppressWarnings("unused")
    private final BigDoors plugin;
    private final WorldGuardPlugin worldGuard;
    private boolean success = false;
    private Method m;

    public WorldGuard6ProtectionCompat(BigDoors plugin)
    {
        this.plugin = plugin;

        Plugin wgPlugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(wgPlugin instanceof WorldGuardPlugin))
        {
            worldGuard = null;
            return;
        }

        worldGuard = (WorldGuardPlugin) wgPlugin;

        try
        {
            m = worldGuard.getClass().getMethod("canBuild", Player.class, Location.class);
            success = true;
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        try
        {
            return (boolean) (m.invoke(worldGuard, player, loc));
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            e.printStackTrace();
        }
        return false;
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

        for (; x1 <= x2; ++x1)
            for (; y1 <= y2; ++y1)
                for (; z1 <= z2; ++z1)
                    if (!canBreakBlock(player, new Location(loc1.getWorld(), x1, y1, z1)))
                        return false;
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
        return worldGuard;
    }
}