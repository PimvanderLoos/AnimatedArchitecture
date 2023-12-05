package nl.pim16aap2.animatedarchitecture.spigot.hooks.worldguard7;

import com.google.common.flogger.LazyArgs;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.FlagValueCalculator;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

/**
 * Protection hook for WorldGuard 7.
 */
@Flogger
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

    private boolean canBreakBlock(com.sk89q.worldedit.util.Location location, LocalPlayer player, Player bukkitPlayer)
    {
        final boolean result = query().testState(location, player, Flags.BLOCK_BREAK, Flags.BLOCK_PLACE);
        if (!result)
        {
            final boolean finer = log.atFiner().isEnabled();
            // Only log the full details on the 'finer' level.
            final var setDetails =
                finer ? LazyArgs.lazy(() -> getApplicableRegionSetDetails(location, player)) : "Skipped";
            final var level = finer ? Level.FINER : Level.FINE;

            log.at(level).log(
                "Player %s is not allowed to break block at %s: Region details: %s",
                lazyFormatPlayerName(bukkitPlayer),
                formatWorldEditLocation(location),
                setDetails
            );
        }
        return result;
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

        return canBreakBlock(toWorldGuardLocation(loc, wgWorld), wgPlayer, player);
    }

    @Override
    public boolean canBreakBlocksBetweenLocs(Player player, World world, Cuboid cuboid)
    {
        log.atInfo().log(
            "Checking if player %s can break blocks between %s and %s",
            lazyFormatPlayerName(player), cuboid.getMin(), cuboid.getMax());

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
                    if (!canBreakBlock(toWorldEditLocation(wgWorld, xPos, yPos, zPos), wgPlayer, player))
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
        // TODO: Make this work for offline players.
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

    /**
     * Format a WorldEdit {@link com.sk89q.worldedit.util.Location} to a string.
     *
     * @param location
     *     The WorldEdit location to format.
     * @return The formatted location.
     */
    private String formatWorldEditLocation(com.sk89q.worldedit.util.Location location)
    {
        return String.format(
            "[%d, %d, %d]",
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
    }

    /**
     * Create a new WorldEdit {@link com.sk89q.worldedit.util.Location} given the provided parameters.
     *
     * @param wgWorld
     *     The WorldEdit world.
     * @param xPos
     *     The x position.
     * @param yPos
     *     The y position.
     * @param zPos
     *     The z position.
     * @return The created WorldEdit location.
     */
    private com.sk89q.worldedit.util.Location toWorldEditLocation(
        com.sk89q.worldedit.world.World wgWorld,
        int xPos, int yPos, int zPos)
    {
        return new com.sk89q.worldedit.util.Location(wgWorld, xPos, yPos, zPos);
    }

    /**
     * Gets the applicable region set details for the provided location and player.
     * <p>
     * For each region, we gather details such as whether it is ignored, the effective value of the flag, the priority,
     * etc.
     *
     * @param location
     *     The location to get the applicable region set details for.
     * @param player
     *     The player to get the applicable region set details for.
     * @return The applicable region set details for the provided location and player.
     */
    private String getApplicableRegionSetDetails(com.sk89q.worldedit.util.Location location, LocalPlayer player)
    {
        final StringBuilder sb = new StringBuilder();

        final Set<ProtectedRegion> regions = query().getApplicableRegions(location).getRegions();

        for (final var flag : List.of(Flags.BLOCK_BREAK, Flags.BLOCK_PLACE))
        {
            sb.append("\nPer-region details for flag ").append(flag.getName()).append(":\n");

            final Set<ProtectedRegion> ignoredParents = new HashSet<>();
            for (final ProtectedRegion region : regions)
            {
                sb.append('\n').append("  Region: ").append(region.getId()).append('\n');

                final boolean isIgnored = ignoredParents.contains(region);
                sb.append("    Ignored: ").append(isIgnored).append('\n');

                final StateFlag.State effectiveValue = FlagValueCalculator.getEffectiveFlagOf(region, flag, player);
                sb.append("    Effective value: ").append(effectiveValue).append('\n');

                final int priority = FlagValueCalculator.getPriorityOf(region);
                sb.append("    Priority: ").append(priority).append('\n');

                final var parents = getParentsOfRegion(region);
                sb.append("    Parents: ").append(parents).append('\n');
                ignoredParents.addAll(parents);
            }
        }

        return sb.toString();
    }

    /**
     * Gets all parents of the provided region.
     *
     * @param region
     *     The region to get the parents of.
     * @return All parents of the provided region.
     */
    private Set<ProtectedRegion> getParentsOfRegion(ProtectedRegion region)
    {
        final Set<ProtectedRegion> parents = new HashSet<>();
        @Nullable ProtectedRegion parent = region.getParent();
        while (parent != null)
        {
            parents.add(parent);
            parent = parent.getParent();
        }
        return parents;
    }
}
