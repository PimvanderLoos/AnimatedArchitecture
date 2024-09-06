package nl.pim16aap2.animatedarchitecture.spigot.hooks.worldguard7;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.BukkitPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.HookPreCheckResult;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IFakePlayer;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.IProtectionHookSpigot;
import nl.pim16aap2.animatedarchitecture.spigot.util.hooks.ProtectionHookContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Protection hook for WorldGuard 7.
 */
@Flogger
public class WorldGuard7ProtectionHook implements IProtectionHookSpigot
{
    private static final StateFlag[] FLAGS = new StateFlag[]{Flags.BLOCK_BREAK, Flags.BLOCK_PLACE, Flags.BUILD};

    private final WorldGuard worldGuard;
    private final WorldGuardPlugin worldGuardPlugin;
    @Getter
    private final ProtectionHookContext context;

    @SuppressWarnings("unused") // Called by reflection.
    public WorldGuard7ProtectionHook(ProtectionHookContext context)
    {
        worldGuard = WorldGuard.getInstance();
        worldGuardPlugin = JavaPlugin.getPlugin(WorldGuardPlugin.class);
        this.context = context;
    }

    private boolean enabledInWorld(com.sk89q.worldedit.world.World world)
    {
        final @Nullable RegionManager regionManager = worldGuard.getPlatform().getRegionContainer().get(world);
        return regionManager != null && regionManager.size() > 0;
    }

    @Override
    public HookPreCheckResult preCheck(Player player, World world)
    {
        if (!enabledInWorld(toWorldGuardWorld(world)))
            return HookPreCheckResult.BYPASS;

        // We don't want to check permissions for fake (offline) players on the main thread.
        if (isFakePlayer(player))
        {
            log.atFinest().log(
                "Player %s is a fake player, skipping sync pre-check.",
                lazyFormatPlayerName(player)
            );
            return HookPreCheckResult.ALLOW;
        }

        final var wgPlayer = toWorldGuardPlayer(player);
        final var wgWorld = toWorldGuardWorld(world);

        final boolean hasBypass = worldGuard.getPlatform().getSessionManager().hasBypass(wgPlayer, wgWorld);
        final var result = hasBypass ? HookPreCheckResult.BYPASS : HookPreCheckResult.ALLOW;
        log.atFiner().log(
            "Sync pre-check for player %s in world '%s': %s",
            lazyFormatPlayerName(player),
            world.getName(),
            result
        );
        return result;
    }

    @Override
    public CompletableFuture<HookPreCheckResult> preCheckAsync(Player player, World world)
    {
        if (!isFakePlayer(player))
        {
            log.atFinest().log(
                "Player %s is not a fake player, skipping async pre-check.",
                lazyFormatPlayerName(player)
            );
            return CompletableFuture.completedFuture(HookPreCheckResult.ALLOW);
        }

        return hasPermissionOffline(world, player, "worldguard.region.bypass." + world.getName())
            .exceptionally(e ->
            {
                log.atSevere().withCause(e).log(
                    "Error while checking permission for player %s in world '%s'.",
                    lazyFormatPlayerName(player),
                    world.getName()
                );
                return false;
            })
            .thenApply(result ->
            {
                final var result0 = result ? HookPreCheckResult.BYPASS : HookPreCheckResult.ALLOW;
                log.atFiner().log(
                    "Async pre-check for player %s in world '%s': %s",
                    lazyFormatPlayerName(player),
                    world.getName(),
                    result0
                );
                return result0;
            });
    }

    @Override
    public CompletableFuture<Boolean> canBreakBlock(Player player, Location loc)
    {
        final var wgWorld = toWorldGuardWorld(loc.getWorld());

        final var wgPlayer = toWorldGuardPlayer(player);
        final var wgLoc = toWorldGuardLocation(loc, wgWorld);

        return CompletableFuture.completedFuture(query().testState(wgLoc, wgPlayer, FLAGS));
    }

    @Override
    public CompletableFuture<Boolean> canBreakBlocksInCuboid(Player player, World world, Cuboid cuboid)
    {
        final var wgWorld = toWorldGuardWorld(world);

        final RegionQuery query = worldGuard.getPlatform().getRegionContainer().createQuery();
        final RegionAssociable regionAssociable = regionAssociableFromPlayer(player);

        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();
        for (int xPos = min.x(); xPos <= max.x(); ++xPos)
            for (int yPos = min.y(); yPos <= max.y(); ++yPos)
                for (int zPos = min.z(); zPos <= max.z(); ++zPos)
                {
                    final com.sk89q.worldedit.util.Location wgLoc =
                        new com.sk89q.worldedit.util.Location(wgWorld, xPos, yPos, zPos);

                    if (!query.testState(wgLoc, regionAssociable, FLAGS))
                        return CompletableFuture.completedFuture(false);
                }
        return CompletableFuture.completedFuture(true);
    }

    private RegionAssociable regionAssociableFromPlayer(Player player)
    {
        if (Bukkit.getPlayer(player.getUniqueId()) == null)
            return worldGuardPlugin.wrapOfflinePlayer(player);
        else
            return new BukkitPlayer(worldGuardPlugin, player);
    }

    private RegionQuery query()
    {
        return worldGuard.getPlatform().getRegionContainer().createQuery();
    }

    private boolean isFakePlayer(Player player)
    {
        return player instanceof IFakePlayer;
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
