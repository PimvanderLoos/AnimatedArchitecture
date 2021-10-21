package nl.pim16aap2.bigDoors.compatibility;

import br.net.fabiozumbi12.RedProtect.Bukkit.API.RedProtectAPI;
import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Compatibility hook for Lands:
 * https://www.spigotmc.org/threads/lands-the-new-way-to-let-players-manage-their-land-create-your-land-today-50-release.304906/
 *
 * @see IProtectionCompat
 * @author Pim
 */
public class RedProtectProtectionCompat implements IProtectionCompat
{
    private boolean success = false;
    private final RedProtect redProtect;
    private final IProtectionCompatDefinition compatDefinition;

    public RedProtectProtectionCompat(IProtectionCompatDefinition compatDefinition,
                                      @SuppressWarnings("unused") JavaPlugin plugin)
    {
        this.compatDefinition = compatDefinition;
        Plugin pRP = Bukkit.getPluginManager().getPlugin("RedProtect");
        if (pRP != null && pRP.isEnabled())
            redProtect = RedProtect.get();
        else
            redProtect = null;
        success = redProtect != null;
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
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        for (int xPos = x1; xPos <= x2; ++xPos)
            for (int yPos = y1; yPos <= y2; ++yPos)
                for (int zPos = z1; zPos <= z2; ++zPos)
                    if (!canBreakBlock(player, new Location(loc1.getWorld(), xPos, yPos, zPos)))
                        return false;
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
        return compatDefinition.getName();
    }
}
