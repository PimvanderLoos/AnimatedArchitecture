package nl.pim16aap2.bigdoors.spigot.util.implementations.glowingblocks;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import nl.pim16aap2.bigdoors.util.IGlowingBlock;
import nl.pim16aap2.bigdoors.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Singleton
public class GlowingBlockSpawner extends Restartable implements IGlowingBlockSpawner, IRestartableHolder
{
    @Getter
    private final Map<PColor, Team> teams = new EnumMap<>(PColor.class);
    private final Set<IRestartable> restartables = new ConcurrentHashMap<IRestartable, Boolean>().keySet();

    private final IGlowingBlockFactory glowingBlockFactory;
    private final IPLogger logger;

    private volatile @Nullable Scoreboard scoreboard;
    /**
     * Keeps track of whether this class (specifically, {@link #scoreboard}) is initialized.
     */
    private volatile boolean isInitialized = false;

    @Inject
    public GlowingBlockSpawner(IRestartableHolder holder, IGlowingBlockFactory glowingBlockFactory, IPLogger logger)
    {
        super(holder);
        this.logger = logger;
        this.glowingBlockFactory = glowingBlockFactory;
    }

    @Override
    public Optional<IGlowingBlock> spawnGlowingBlock(IPPlayer player, IPWorld world, int time, TimeUnit timeUnit,
                                                     double x, double y, double z, PColor pColor)
    {
        ensureInitialized();
        if (scoreboard == null)
        {
            logger.warn("Failed to spawn glowing block: Scoreboard is null!");
            return Optional.empty();
        }

        if (teams.get(pColor) == null)
        {
            // FINER because it will already have been logged on startup.
            logger.logMessage(Level.FINER, "GlowingBlock Color " + pColor.name() + " was not registered properly!");
            return Optional.empty();
        }

        final long ticks = TimeUnit.MILLISECONDS.convert(time, timeUnit) / 50;
        if (ticks == 0)
        {
            logger.logThrowable(
                new IllegalArgumentException("Invalid duration of " + time + " " + timeUnit.name() + " provided! "));
            return Optional.empty();
        }

        final @Nullable Player spigotPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (spigotPlayer == null)
        {
            logger.logThrowable(new NullPointerException(), "Player " + player +
                " does not appear to be online! They will not receive any GlowingBlock packets!");
            return Optional.empty();
        }

        final @Nullable World spigotWorld = SpigotAdapter.getBukkitWorld(world);
        if (spigotWorld == null)
        {
            logger.logThrowable(new NullPointerException(), "World " + world +
                " does not appear to be online! No Glowing Blocks can be spawned here!");
            return Optional.empty();
        }

        final Optional<IGlowingBlock> blockOpt =
            glowingBlockFactory.createGlowingBlock(spigotPlayer, spigotWorld, this, logger);
        blockOpt.ifPresent(block -> block.spawn(pColor, x, y, z, ticks));
        return blockOpt;
    }

    @Override
    public void registerRestartable(IRestartable restartable)
    {
        restartables.add(restartable);
    }

    @Override
    public boolean isRestartableRegistered(IRestartable restartable)
    {
        return restartables.contains(restartable);
    }

    @Override
    public void deregisterRestartable(IRestartable restartable)
    {
        restartables.remove(restartable);
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

    private void init()
    {
        final ScoreboardManager scoreboardManager = Util.requireNonNull(Bukkit.getServer().getScoreboardManager(),
                                                                        "scoreboardManager");
        scoreboard = Util.requireNonNull(scoreboardManager.getMainScoreboard(), "scoreboard");
        //noinspection ConstantConditions
        registerTeams(scoreboard);
    }

    /**
     * Ensures this class is initialized.
     * <p>
     * This method is required because this class is initialized lazily, as the required scoreboard isn't available
     * until the first world has been loaded (as per Spigot documentation), while this class may or may not be
     * instantiated before then.
     */
    private void ensureInitialized()
    {
        // Use double-checked locking to avoid synchronization when not needed (99+% of all cases).
        if (!isInitialized)
        {
            synchronized (this)
            {
                if (!isInitialized)
                {
                    init();
                    isInitialized = true;
                }
            }
        }
    }

    @Override
    public void restart()
    {
        teams.forEach((color, team) -> team.unregister());
        teams.clear();
        registerTeams(Util.requireNonNull(scoreboard, "Scoreboard"));
        restartables.forEach(IRestartable::restart);
    }

    @Override
    public void shutdown()
    {
        teams.forEach((color, team) -> team.unregister());
        teams.clear();
        restartables.forEach(IRestartable::shutdown);
    }
}
