package nl.pim16aap2.animatedarchitecture.spigot.core.implementations;

import com.google.common.flogger.StackSize;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.api.Color;
import nl.pim16aap2.animatedarchitecture.core.api.HighlightedBlockSpawner;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IHighlightedBlock;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.IRestartable;
import nl.pim16aap2.animatedarchitecture.core.api.restartable.RestartableHolder;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.spigot.core.AnimatedArchitecturePlugin;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.BlockDisplayHelper;
import nl.pim16aap2.animatedarchitecture.spigot.core.animation.HighlightedBlockDisplay;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotAdapter;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Flogger
public class HighlightedBlockSpawnerSpigot extends HighlightedBlockSpawner implements IRestartable
{
    private static final BukkitRunnable NULL_RUNNABLE = new BukkitRunnable()
    {
        @Override
        public void run()
        {
            throw new UnsupportedOperationException();
        }
    };

    @Getter
    private final Map<Color, Team> teams = new EnumMap<>(Color.class);

    private final Map<IHighlightedBlock, BukkitRunnable> spawnedBlocks = new ConcurrentHashMap<>(128);

    private final AnimatedArchitecturePlugin plugin;

    private @Nullable Scoreboard scoreboard;

    @Getter(AccessLevel.PROTECTED)
    private final IExecutor executor;

    private final BlockDisplayHelper blockDisplayHelper;

    @Inject HighlightedBlockSpawnerSpigot(
        RestartableHolder holder,
        AnimatedArchitecturePlugin plugin,
        BlockDisplayHelper blockDisplayHelper,
        IExecutor executor)
    {
        this.plugin = plugin;
        this.executor = executor;
        this.blockDisplayHelper = blockDisplayHelper;
        holder.registerRestartable(this);
    }

    @Override
    public Optional<IHighlightedBlock> spawnHighlightedBlock(
        IPlayer player, IWorld world, @Nullable Duration duration, RotatedPosition rotatedPosition, Color color)
    {
        executor.assertMainThread("Glowing blocks must be spawned on the main thread!");

        if (scoreboard == null)
        {
            log.atWarning().log("Failed to spawn glowing block: Scoreboard is null!");
            return Optional.empty();
        }

        if (teams.get(color) == null)
        {
            // FINER because it will already have been logged on startup.
            log.atFiner().log("GlowingBlock Color %s was not registered properly!", color.name());
            return Optional.empty();
        }

        final @Nullable Long time = duration == null ? null : Math.max(50, duration.toMillis());

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

        final HighlightedBlockDisplay block = Objects.requireNonNull(newPreviewBlock(world, rotatedPosition, color));
        final @Nullable Entity entity = block.getEntity();
        if (entity == null || !setEntityTeam(entity, color))
        {
            block.kill();
            log.atSevere().log("Failed to create glowing entity!");
            return Optional.empty();
        }
        //noinspection UnstableApiUsage
        spigotPlayer.showEntity(plugin, entity);

        onBlockSpawn(block, time);
        return Optional.of(block);
    }

    private HighlightedBlockDisplay newPreviewBlock(IWorld world, RotatedPosition rotatedPosition, Color color)
    {
        final HighlightedBlockDisplay ret =
            new HighlightedBlockDisplay(blockDisplayHelper, executor, rotatedPosition, world, color);
        ret.spawn();
        return ret;
    }

    private boolean setEntityTeam(Entity entity, Color color)
    {
        final @Nullable Team team = teams.get(color);
        if (team == null)
        {
            log.atWarning().log("Failed to spawn glowing block: Could not find team for color: %s", color);
            return false;
        }
        team.addEntry(entity.getUniqueId().toString());
        entity.setGlowing(true);
        return true;
    }

    private void onBlockSpawn(IHighlightedBlock block, @Nullable Long time)
    {
        if (time == null)
            spawnedBlocks.put(block, NULL_RUNNABLE);
        else
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
            executor.runSyncLater(killTask, time);
            spawnedBlocks.put(block, killTask);
        }
    }

    /**
     * Registers a new team with a specific color.
     *
     * @param color
     *     The color to register the team for.
     */
    private void registerTeam(Color color, Scoreboard scoreboard)
    {
        final ChatColor chatColor = SpigotUtil.toBukkitColor(color);
        final String name = "AnimatedArchitecture" + color.ordinal();
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
        for (final Color col : Color.values())
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
            try
            {
                final BukkitRunnable task = entry.getValue();
                if (task != NULL_RUNNABLE)
                    task.cancel();
            }
            catch (IllegalStateException e)
            {
                log.atFine().withCause(e).log("Failed to cancel task for glowing block!");
            }
            entry.getKey().kill();
        }
        spawnedBlocks.clear();
    }
}
