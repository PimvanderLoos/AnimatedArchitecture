package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IGlowingBlock;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IRestartableHolder;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;

/**
 * V1_15_R1 implementation of {@link IGlowingBlockSpawner}.
 *
 * @author Pim
 * @see IGlowingBlockSpawner
 */
public class GlowingBlockSpawner_V1_15_R1 extends Restartable implements IGlowingBlockSpawner
{
    @Nullable
    private static GlowingBlockSpawner_V1_15_R1 instance;
    @NotNull
    private final Map<PColor, Team> teams;
    @NotNull
    private final Scoreboard scoreboard;
    @NotNull
    private final PLogger plogger;

    /**
     * Constructs a glowing block spawner.
     *
     * @param holder  The holder of this restartable. I.e., the object that manages the restarting / stopping of this
     *                object.
     * @param plogger The logger object to log to.
     */
    private GlowingBlockSpawner_V1_15_R1(final @NotNull IRestartableHolder holder, final @NotNull PLogger plogger)
    {
        super(holder);
        this.plogger = plogger;
        teams = new EnumMap<>(PColor.class);
        scoreboard = org.bukkit.Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        init();
    }

    /**
     * Obtains the instance of this class.
     *
     * @return The instance of this class.
     */
    @Nullable
    public static GlowingBlockSpawner_V1_15_R1 get()
    {
        return instance;
    }

    /**
     * Initializes the {@link GlowingBlockSpawner_V1_15_R1}. If it has already been initialized, it'll return that
     * instance instead.
     *
     * @param holder  The holder of this restartable. I.e., the object that manages the restarting / stopping of this
     *                object.
     * @param plogger The logger object to log to.
     * @return The instance of this {@link GlowingBlockSpawner_V1_15_R1}.
     */
    @NotNull
    public static GlowingBlockSpawner_V1_15_R1 init(final @NotNull IRestartableHolder holder,
                                                    final @NotNull PLogger plogger)
    {
        return instance == null ? instance = new GlowingBlockSpawner_V1_15_R1(holder, plogger) : instance;
    }

    /**
     * Initializes all teams.
     */
    private void init()
    {
        for (final PColor col : PColor.values())
            try
            {
                registerTeam(col);
            }
            catch (Exception e)
            {
                plogger.logException(e, "Failed to register color: " + col.name());
            }
    }

    /**
     * Registers a new team with a specific color.
     *
     * @param color The color to register the team for.
     */
    private void registerTeam(final @NotNull PColor color)
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

    /** {@inheritDoc} */
    @Override
    public void restart()
    {
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown()
    {
        teams.forEach((K, V) -> V.unregister());
        teams.clear();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public IGlowingBlock spawnGlowingBlock(final @NotNull IPPlayer pPlayer, final @NotNull UUID world, final int time,
                                           final double x, final double y, final double z, final @NotNull PColor color)
    {
        final Player player = Bukkit.getPlayer(pPlayer.getUUID());
        final World bukkitWorld = Bukkit.getWorld(world);
        if (player == null || bukkitWorld == null)
        {
            plogger.logException(new NullPointerException(),
                                 (player == null ? "Player" : "bukkitWorld") + " unexpectedly null!");
            return null;
        }

        final IGlowingBlock glowingBlock = new GlowingBlock_V1_15_R1(player, bukkitWorld, color, x, y, z);
        BigDoors.get().getPlatform().newPExecutor().runSyncLater(new TimerTask()
        {
            @Override
            public void run()
            {
                glowingBlock.kill();
            }
        }, time * 20);

        return glowingBlock;
    }

    /**
     * Gets all the registered teams.
     *
     * @return All the registered teams.
     */
    @NotNull
    Map<PColor, Team> getTeams()
    {
        return teams;
    }
}
