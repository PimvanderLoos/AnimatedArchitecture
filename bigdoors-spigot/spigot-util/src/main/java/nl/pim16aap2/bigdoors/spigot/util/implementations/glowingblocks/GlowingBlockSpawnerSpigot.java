package nl.pim16aap2.bigdoors.spigot.util.implementations.glowingblocks;

import com.google.common.flogger.StackSize;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.RestartableHolder;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import nl.pim16aap2.bigdoors.util.IGlowingBlock;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Flogger
public class GlowingBlockSpawnerSpigot extends GlowingBlockSpawner implements IRestartable
{
    @Getter
    private final Map<PColor, Team> teams = new EnumMap<>(PColor.class);

    private final Map<IGlowingBlock, BukkitRunnable> spawnedBlocks = new ConcurrentHashMap<>(128);

    private final IGlowingBlockFactory glowingBlockFactory;

    private @Nullable Scoreboard scoreboard;

    private final IPExecutor executor;

    @Inject
    public GlowingBlockSpawnerSpigot(
        RestartableHolder holder, IGlowingBlockFactory glowingBlockFactory, IPExecutor executor)
    {
        this.glowingBlockFactory = glowingBlockFactory;
        this.executor = executor;
        holder.registerRestartable(this);
    }

    @Override
    public Optional<IGlowingBlock> spawnGlowingBlock(
        IPPlayer player, IPWorld world, Duration duration, double x, double y, double z, PColor pColor)
    {
        if (scoreboard == null)
        {
            log.atWarning().log("Failed to spawn glowing block: Scoreboard is null!");
            return Optional.empty();
        }

        if (teams.get(pColor) == null)
        {
            // FINER because it will already have been logged on startup.
            log.atFiner().log("GlowingBlock Color %s was not registered properly!", pColor.name());
            return Optional.empty();
        }

        final long ticks = SpigotUtil.durationToTicks(duration);
        if (ticks < 5)
        {
            log.atSevere().withStackTrace(StackSize.FULL).log("Invalid duration of %d ticks!", ticks);
            return Optional.empty();
        }

        final @Nullable Player spigotPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (spigotPlayer == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Player %s does not appear to be online! They will not receive any GlowingBlock packets!", player);
            return Optional.empty();
        }

        final @Nullable World spigotWorld = SpigotAdapter.getBukkitWorld(world);
        if (spigotWorld == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("World %s does not appear to be online! No Glowing Blocks can be spawned here!", world);
            return Optional.empty();
        }

        final Optional<IGlowingBlock> blockOpt =
            glowingBlockFactory.createGlowingBlock(spigotPlayer, spigotWorld, pColor, x, y, z, teams);
        blockOpt.ifPresent(block -> onBlockSpawn(block, ticks));
        return blockOpt;
    }

    private void onBlockSpawn(IGlowingBlock block, long ticks)
    {
        final BukkitRunnable killTask = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                block.kill();
                spawnedBlocks.remove(block);
            }
        };
        spawnedBlocks.put(block, killTask);
        executor.runAsyncLater(killTask, ticks);
    }

    /**
     * Registers a new team with a specific color.
     *
     * @param color
     *     The color to register the team for.
     */
    private void registerTeam(PColor color, Scoreboard scoreboard)
    {
        final ChatColor chatColor = SpigotUtil.toBukkitColor(color);
        final String name = "BigDoors" + color.ordinal();
        // Try to get an existing team, in case something had gone wrong unregistering them last time.
        @Nullable Team team = scoreboard.getTeam(name);
        if (team == null)
            team = scoreboard.registerNewTeam(name);
        team.setColor(chatColor);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        teams.put(color, team);
    }

    /**
     * Initializes all teams.
     */
    private void registerTeams(Scoreboard scoreboard)
    {
        for (final PColor col : PColor.values())
            registerTeam(col, scoreboard);
    }

    @Override
    public synchronized void initialize()
    {
        final ScoreboardManager scoreboardManager = Util.requireNonNull(Bukkit.getServer().getScoreboardManager(),
                                                                        "scoreboardManager");
        scoreboard = Util.requireNonNull(scoreboardManager.getMainScoreboard(), "scoreboard");
        registerTeams(scoreboard);
    }

    @Override
    public synchronized void shutDown()
    {
        teams.forEach((color, team) -> team.unregister());
        teams.clear();

        killAllSpawnedBlocks();

        scoreboard = null;
    }

    private void killAllSpawnedBlocks()
    {
        for (final var entry : spawnedBlocks.entrySet())
        {
            entry.getValue().cancel();
            entry.getKey().kill();
        }
        spawnedBlocks.clear();
    }
}
