package nl.pim16aap2.animatedarchitecture.compatibility.redprotect;

import br.net.fabiozumbi12.RedProtect.Bukkit.API.RedProtectAPI;
import br.net.fabiozumbi12.RedProtect.Bukkit.RedProtect;
import br.net.fabiozumbi12.RedProtect.Bukkit.Region;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
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
    public boolean canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid)
    {
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        for (int xPos = min.x(); xPos <= max.x(); ++xPos)
            for (int yPos = min.y(); yPos <= max.y(); ++yPos)
                for (int zPos = min.z(); zPos <= max.z(); ++zPos)
                    if (!canBreakBlock(player, new Location(world, xPos, yPos, zPos)))
                        return false;
        return true;
    }

    @Override
    public String getName()
    {
        return context.getSpecification().getName();
    }
}
