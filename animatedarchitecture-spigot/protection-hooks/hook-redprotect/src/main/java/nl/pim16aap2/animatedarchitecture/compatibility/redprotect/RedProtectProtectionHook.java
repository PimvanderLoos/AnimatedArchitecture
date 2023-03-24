package nl.pim16aap2.animatedarchitecture.compatibility.redprotect;

import br.net.fabiozumbi12.RedProtect.Bukkit.API.RedProtectAPI;
import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Protection hook for RedProtect.
 */
public class RedProtectProtectionHook implements IProtectionHookSpigot
{
    private final RedProtect redProtect;
    private final ProtectionHookContext context;

    @SuppressWarnings("unused") // Called by reflection.
    public RedProtectProtectionHook(ProtectionHookContext context)
    {
        this.context = context;
        this.redProtect = JavaPlugin.getPlugin(RedProtect.class);
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        try
        {
            final RedProtectAPI rpAPI = redProtect.getAPI();
            final Region rpRegion = rpAPI.getRegion(loc);
            return rpRegion == null || rpRegion.canBuild(player);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, Location loc1, Location loc2)
    {
        final int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        final int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        final int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        final int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        final int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        final int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        for (int xPos = x1; xPos <= x2; ++xPos)
            for (int yPos = y1; yPos <= y2; ++yPos)
                for (int zPos = z1; zPos <= z2; ++zPos)
                    if (!canBreakBlock(player, new Location(loc1.getWorld(), xPos, yPos, zPos)))
                        return false;
        return true;
    }

    @Override
    public String getName()
    {
        return context.getSpecification().getName();
    }
}
