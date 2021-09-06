package nl.pim16aap2.bigdoors.spigot.util;

import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.restartable.Restartable;
import nl.pim16aap2.bigdoors.spigot.util.api.BigDoorsSpigotAbstract;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import nl.pim16aap2.bigdoors.spigot.util.api.ISpigotPlatform;
import nl.pim16aap2.bigdoors.util.IGlowingBlock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

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
    private final Set<IRestartable> restartables = new ConcurrentHashMap<IRestartable, Boolean>().keySet();
    @Getter
    private final Map<PColor, Team> teams = new EnumMap<>(PColor.class);
    private final Scoreboard scoreboard;
    private final IGlowingBlockFactory glowingBlockFactory;

    public GlowingBlockSpawner(IRestartableHolder holder)
        throws Exception
    {
        super(holder);
        final @Nullable ScoreboardManager scoreBoardManager = Bukkit.getServer().getScoreboardManager();
        if (scoreBoardManager == null)
            throw new Exception("Could not find a ScoreBoardManager! No glowing blocks can be spawned!");

        scoreboard = scoreBoardManager.getMainScoreboard();

        final IBigDoorsPlatform platform = BigDoors.get().getPlatform();
        if (!(platform instanceof BigDoorsSpigotAbstract))
            throw new Exception("Spigot's GlowingBlockSpawner can only be used with the Spigot Platform!");

        final @Nullable ISpigotPlatform spigotPlatform = ((BigDoorsSpigotAbstract) platform).getPlatformManagerSpigot()
                                                                                            .getSpigotPlatform();
        if (spigotPlatform == null)
            throw new Exception("No valid Spigot platform was found!");

        glowingBlockFactory = spigotPlatform.getGlowingBlockFactory();

        registerTeams();
    }

    /**
     * Initializes all teams.
     */
    private void registerTeams()
    {
        for (final PColor col : PColor.values())
            registerTeam(col, scoreboard);
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

    @Override
    public void restart()
    {
        teams.forEach((color, team) -> team.unregister());
        teams.clear();
        registerTeams();
        restartables.forEach(IRestartable::restart);
    }

    @Override
    public void shutdown()
    {
        teams.forEach((color, team) -> team.unregister());
        teams.clear();
        restartables.forEach(IRestartable::shutdown);
    }

    @Override
    public Optional<IGlowingBlock> spawnGlowingBlock(IPPlayer player, IPWorld world, int time, TimeUnit timeUnit,
                                                     double x, double y, double z, PColor pColor)
    {
        if (teams.get(pColor) == null)
        {
            // FINER because it will already have been logged on startup.
            BigDoors.get().getPLogger()
                    .logMessage(Level.FINER, "GlowingBlock Color " + pColor.name() + " was not registered properly!");
            return Optional.empty();
        }

        final long ticks = TimeUnit.MILLISECONDS.convert(time, timeUnit) / 50;
        if (ticks == 0)
        {
            BigDoors.get().getPLogger().logThrowable(
                new IllegalArgumentException("Invalid duration of " + time + " " + timeUnit.name() + " provided! "));
            return Optional.empty();
        }

        final @Nullable Player spigotPlayer = SpigotAdapter.getBukkitPlayer(player);
        if (spigotPlayer == null)
        {
            BigDoors.get().getPLogger().logThrowable(new NullPointerException(), "Player " + player +
                " does not appear to be online! They will not receive any GlowingBlock packets!");
            return Optional.empty();
        }

        final @Nullable World spigotWorld = SpigotAdapter.getBukkitWorld(world);
        if (spigotWorld == null)
        {
            BigDoors.get().getPLogger().logThrowable(new NullPointerException(), "World " + world.toString() +
                " does not appear to be online! No Glowing Blocks can be spawned here!");
            return Optional.empty();
        }

        final Optional<IGlowingBlock> blockOpt =
            glowingBlockFactory.createGlowingBlock(spigotPlayer, spigotWorld, this);
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
}
