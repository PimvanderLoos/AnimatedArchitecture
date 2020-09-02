package nl.pim16aap2.bigdoors.spigot.util;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IRestartable;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.spigot.util.api.BigDoorsSpigotAbstract;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;
import nl.pim16aap2.bigdoors.util.IGlowingBlock;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class GlowingBlockManager extends Restartable implements IGlowingBlockSpawner, IRestartableHolder
{
    private static final @NotNull GlowingBlockManager INSTANCE = new GlowingBlockManager();
    private final @NotNull Map<IRestartable, Boolean> restartables = new ConcurrentHashMap<>();
    @Getter
    private final @NotNull Map<PColor, Team> teams = new EnumMap<>(PColor.class);
    private final @Nullable Scoreboard scoreboard;
    private final @Nullable IGlowingBlockFactory glowingBlockFactory;

    private GlowingBlockManager()
    {
        super(BigDoors.get());
        final @Nullable ScoreboardManager scoreBoardManager = Bukkit.getServer().getScoreboardManager();
        if (scoreBoardManager == null)
        {
            PLogger.get().logThrowable(
                new IllegalStateException("Could not find a ScoreBoardManager! No glowing blocks can be spawned!"));
            glowingBlockFactory = null;
            scoreboard = null;
            return;
        }
        scoreboard = scoreBoardManager.getMainScoreboard();


        final @NotNull IBigDoorsPlatform platform = BigDoors.get().getPlatform();
        if (!(platform instanceof BigDoorsSpigotAbstract))
        {
            PLogger.get().logThrowable(
                new IllegalStateException("Spigot's GlowingBlockManager can only be used with the Spigot Platform!"));
            glowingBlockFactory = null;
            return;
        }

        final @Nullable ISpigotPlatform spigotPlatform = ((BigDoorsSpigotAbstract) platform).getPlatformManagerSpigot()
                                                                                            .getSpigotPlatform();
        if (spigotPlatform == null)
        {
            PLogger.get().logThrowableSilently(Level.FINE,
                                               new NullPointerException("No valid Spigot platform was found!"));
            glowingBlockFactory = null;
            return;
        }

        glowingBlockFactory = spigotPlatform.getGlowingBlockFactory();
        init();
    }

    /**
     * Obtains the instance of this class.
     *
     * @return The instance of this class.
     */
    public static @NotNull GlowingBlockManager get()
    {
        return INSTANCE;
    }

    /**
     * Initializes all teams.
     */
    private void init()
    {
        if (scoreboard == null)
        {
            PLogger.get()
                   .logMessage(Level.FINE, "Skipping GlowingBlockManager team registration: No ScoreBoard found!");
            return;
        }

        for (final @NotNull PColor col : PColor.values())
            try
            {
                registerTeam(col, scoreboard);
            }
            catch (Exception e)
            {
                PLogger.get().logThrowable(e, "Failed to register color: " + col.name());
            }
    }

    /**
     * Registers a new team with a specific color.
     *
     * @param color The color to register the team for.
     */
    private void registerTeam(final @NotNull PColor color, final @NotNull Scoreboard scoreboard)
    {
        final @NotNull ChatColor chatColor = SpigotUtil.toBukkitColor(color);
        final String name = "BigDoors" + color.ordinal();
        // Try to get an existing team, in case something had gone wrong unregistering them last time.
        Team team = scoreboard.getTeam(name);
        if (team == null)
            team = scoreboard.registerNewTeam(name);
        team.setColor(chatColor);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        teams.put(color, team);
    }

    @Override
    public void restart()
    {
        teams.forEach((K, V) -> V.unregister());
        teams.clear();
        init();
        restartables.forEach((K, V) -> K.restart());
    }

    @Override
    public void shutdown()
    {
        teams.forEach((K, V) -> V.unregister());
        teams.clear();
        restartables.forEach((K, V) -> K.shutdown());
    }

    @Override
    public @NotNull Optional<IGlowingBlock> spawnGlowingBlock(@NotNull IPPlayer player, @NotNull IPWorld world,
                                                              final int time, final @NotNull TimeUnit timeUnit,
                                                              final double x, final double y, final double z,
                                                              final @NotNull PColor pColor)
    {
        if (glowingBlockFactory == null)
        {
            // FINER because it will already have been logged on startup.
            PLogger.get().logMessage(Level.FINER, "GlowingBlockFactory was not initialized!");
            return Optional.empty();
        }
        if (teams.get(pColor) == null)
        {
            // FINER because it will already have been logged on startup.
            PLogger.get()
                   .logMessage(Level.FINER, "GlowingBlock Color " + pColor.name() + " was not registered properly!");
            return Optional.empty();
        }

        final long ticks = TimeUnit.MILLISECONDS.convert(time, timeUnit) / 50;
        if (ticks == 0)
        {
            PLogger.get().logThrowable(
                new IllegalArgumentException("Invalid duration of " + time + " " + timeUnit.name() + " provided! "));
            return Optional.empty();
        }

        final @Nullable Player spigotPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (spigotPlayer == null)
        {
            PLogger.get().logThrowable(new NullPointerException(), "Player " + player.toString() +
                " does not appear to be online! They will not receive any GlowingBlock packets!");
            return Optional.empty();
        }

        final @Nullable World spigotWorld = SpigotAdapter.getBukkitWorld(world);
        if (spigotWorld == null)
        {
            PLogger.get().logThrowable(new NullPointerException(), "World " + world.toString() +
                " does not appear to be online! No Glowing Blocks can be spawned here!");
            return Optional.empty();
        }

        final @NotNull IGlowingBlock glowingBlock = glowingBlockFactory
            .createGlowingBlock(spigotPlayer, spigotWorld, this);
        
        glowingBlock.spawn(pColor, x, y, z, ticks);
        return Optional.of(glowingBlock);
    }

    @Override
    public void registerRestartable(final @NotNull IRestartable restartable)
    {
        restartables.put(restartable, true);
    }

    @Override
    public boolean isRestartableRegistered(final @NotNull IRestartable restartable)
    {
        return restartables.containsKey(restartable);
    }

    @Override
    public void deregisterRestartable(@NotNull IRestartable restartable)
    {
        restartables.remove(restartable);
    }
}
