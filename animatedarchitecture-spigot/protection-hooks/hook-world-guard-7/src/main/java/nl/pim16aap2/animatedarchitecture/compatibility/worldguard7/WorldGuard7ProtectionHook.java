package nl.pim16aap2.animatedarchitecture.compatibility.worldguard7;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.compatibility.ProtectionHookContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Protection hook for WorldGuard 7.
 */
public class WorldGuard7ProtectionHook implements IProtectionHookSpigot
{
    private static final StateFlag[] FLAGS = new StateFlag[]{Flags.BLOCK_BREAK, Flags.BLOCK_PLACE, Flags.BUILD};

    private final WorldGuard worldGuard;
    private final WorldGuardPlugin worldGuardPlugin;

    @SuppressWarnings("unused") // Called by reflection.
    public WorldGuard7ProtectionHook(ProtectionHookContext context)
    {
        worldGuard = WorldGuard.getInstance();
        worldGuardPlugin = JavaPlugin.getPlugin(WorldGuardPlugin.class);
    }

    private boolean enabledInWorld(com.sk89q.worldedit.world.World world)
    {
        final @Nullable RegionManager regionManager = worldGuard.getPlatform().getRegionContainer().get(world);
        return regionManager != null && regionManager.size() > 0;
    }

    private com.sk89q.worldedit.world.World toWorldGuardWorld(@Nullable World world)
    {
        return BukkitAdapter.adapt(Objects.requireNonNull(world, "World cannot be null!"));
    }

    private RegionAssociable regionAssociableFromPlayer(Player player)
    {
        if (Bukkit.getPlayer(player.getUniqueId()) == null)
            return worldGuardPlugin.wrapOfflinePlayer(player);
        else
            return new BukkitPlayer(worldGuardPlugin, player);
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        if (!enabledInWorld(toWorldGuardWorld(loc.getWorld())))
            return true;

        final RegionQuery query = worldGuard.getPlatform().getRegionContainer().createQuery();
        final com.sk89q.worldedit.world.World world = toWorldGuardWorld(loc.getWorld());
        final com.sk89q.worldedit.util.Location wgLoc =
            new com.sk89q.worldedit.util.Location(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        return query.testState(wgLoc, regionAssociableFromPlayer(player), FLAGS);
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid)
    {
        final com.sk89q.worldedit.world.World wgWorld = toWorldGuardWorld(world);
        if (!enabledInWorld(wgWorld))
            return true;

        final RegionAssociable regionAssociable = regionAssociableFromPlayer(player);

        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        for (int xPos = min.x(); xPos <= max.x(); ++xPos)
            for (int yPos = min.y(); yPos <= max.y(); ++yPos)
                for (int zPos = min.z(); zPos <= max.z(); ++zPos)
                {
                    final var wgLoc = new com.sk89q.worldedit.util.Location(wgWorld, xPos, yPos, zPos);
                    if (!query().testState(wgLoc, regionAssociable, FLAGS))
                        return false;
                }
        return true;
    }

    @Override
    public String getName()
    {
        return worldGuardPlugin.getName();
    }
}
