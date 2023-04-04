package nl.pim16aap2.animatedarchitecture.spigot.hooks.worldguard7;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.ProtectionHookContext;
import nl.pim16aap2.util.reflection.ReflectionBuilder;
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
    /**
     * The package-private class "{@code BukkitOfflinePlayer}" is a partial implementation of {@link LocalPlayer} that
     * throws {@link UnsupportedOperationException}s when accessing certain methods.
     * <p>
     * By getting the class object itself, we can ensure we do not make calls to those unsupported methods.
     */
    private static final Class<?> CLASS_BUKKIT_OFFLINE_PLAYER =
        ReflectionBuilder.findClass("com.sk89q.worldguard.bukkit.BukkitOfflinePlayer").get();

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

    private boolean canBreakBlock(com.sk89q.worldedit.util.Location location, LocalPlayer player)
    {
        final var set = query().getApplicableRegions(location);
        return set.queryState(player, Flags.BLOCK_BREAK, Flags.BLOCK_PLACE) != StateFlag.State.DENY;
    }

    @Override
    public boolean canBreakBlock(Player player, Location loc)
    {
        final var wgWorld = toWorldGuardWorld(loc.getWorld());
        if (!enabledInWorld(wgWorld))
            return true;

        final var wgPlayer = toWorldGuardPlayer(player);
        if (canBypass(wgPlayer, wgWorld))
            return true;

        return canBreakBlock(toWorldGuardLocation(loc, wgWorld), wgPlayer);
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid)
    {
        final var wgWorld = toWorldGuardWorld(world);
        if (!enabledInWorld(wgWorld))
            return true;

        final var wgPlayer = toWorldGuardPlayer(player);
        if (canBypass(wgPlayer, wgWorld))
            return true;

        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        for (int xPos = min.x(); xPos <= max.x(); ++xPos)
            for (int yPos = min.y(); yPos <= max.y(); ++yPos)
                for (int zPos = min.z(); zPos <= max.z(); ++zPos)
                    if (!canBreakBlock(new com.sk89q.worldedit.util.Location(wgWorld, xPos, yPos, zPos), wgPlayer))
                        return false;
        return true;
    }

    @Override
    public String getName()
    {
        return worldGuardPlugin.getName();
    }

    private RegionQuery query()
    {
        return worldGuard.getPlatform().getRegionContainer().createQuery();
    }

    private boolean canBypass(LocalPlayer player, com.sk89q.worldedit.world.World world)
    {
        return player.getClass() != CLASS_BUKKIT_OFFLINE_PLAYER &&
            worldGuard.getPlatform().getSessionManager().hasBypass(player, world);
    }

    private LocalPlayer toWorldGuardPlayer(Player player)
    {
        return worldGuardPlugin.wrapPlayer(player, true);
    }

    private com.sk89q.worldedit.world.World toWorldGuardWorld(@Nullable World world)
    {
        return BukkitAdapter.adapt(Objects.requireNonNull(world, "World cannot be null!"));
    }

    private com.sk89q.worldedit.util.Location toWorldGuardLocation(Location loc, com.sk89q.worldedit.world.World world)
    {
        return new com.sk89q.worldedit.util.Location(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
