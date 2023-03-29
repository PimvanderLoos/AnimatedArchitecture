package nl.pim16aap2.animatedarchitecture.compatibility.towny;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.ProtectionHookContext;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Protection hook for Towny.
 */
public class TownyProtectionHook implements IProtectionHookSpigot
{
    private final ProtectionHookContext context;

    @SuppressWarnings("unused") // Called by reflection.
    public TownyProtectionHook(ProtectionHookContext context)
    {
        this.context = context;
        Objects.requireNonNull(TownyAPI.getInstance());
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        return PlayerCacheUtil.getCachePermission(player, loc, loc.getBlock().getType(), ActionType.DESTROY);
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


