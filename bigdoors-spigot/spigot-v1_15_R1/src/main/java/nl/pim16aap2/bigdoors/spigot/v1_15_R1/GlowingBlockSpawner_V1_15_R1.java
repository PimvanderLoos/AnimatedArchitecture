package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import net.minecraft.server.v1_15_R1.EntityMagmaCube;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_15_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_15_R1.PlayerConnection;
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
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * V1_15_R1 implementation of {@link IGlowingBlockSpawner}.
 *
 * @author Pim
 * @see IGlowingBlockSpawner
 */
public class GlowingBlockSpawner_V1_15_R1 extends Restartable implements IGlowingBlockSpawner
{
    private final Map<ChatColor, Team> teams;
    private final Scoreboard scoreboard;
    private final PLogger plogger;

    /**
     * Constructs a glowing block spawner.
     *
     * @param holder  The holder of this restartable. I.e., the object that manages the restarting / stopping of this
     *                object.
     * @param plogger The logger object to log to.
     */
    public GlowingBlockSpawner_V1_15_R1(final @NotNull IRestartableHolder holder, final @NotNull PLogger plogger)
    {
        super(holder);
        this.plogger = plogger;
        teams = new EnumMap<>(ChatColor.class);
        scoreboard = org.bukkit.Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        init();
    }

    /**
     * Initializes all teams.
     */
    private void init()
    {
        for (final ChatColor col : ChatColor.values())
            if (col.isColor())
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
    private void registerTeam(final @NotNull ChatColor color)
    {
        final String name = "BigDoors" + color.ordinal();
        // Try to get an existing team, in case something had gone wrong unregistering them last time.
        Team team = scoreboard.getTeam(name);
        if (team == null)
            team = scoreboard.registerNewTeam(name);
        team.setColor(color);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        teams.put(color, team);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {
        teams.forEach((K, V) -> V.unregister());
        teams.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void spawnGlowinBlock(final @NotNull IPPlayer player, final @NotNull UUID world, final long time,
                                 final double x, final double y, final double z)
    {
        spawnGlowinBlock(player, world, time, x, y, z, PColor.WHITE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void spawnGlowinBlock(final @NotNull IPPlayer pPlayer, final @NotNull UUID world, final long time,
                                 final double x, final double y, final double z, final @NotNull PColor pColor)
    {
        final ChatColor color = SpigotUtil.toBukkitColor(pColor);
        if (!teams.containsKey(color))
        {
            plogger.logException(new IllegalArgumentException("Unsupported color: " + color.name()));
            return;
        }

        final Player player = Bukkit.getPlayer(pPlayer.getUUID());
        final World bukkitWorld = Bukkit.getWorld(world);
        if (player == null || bukkitWorld == null)
        {
            plogger.logException(new NullPointerException(),
                                 (player == null ? "Player" : "bukkitWorld") + " unexpectedly null!");
            return;
        }

        new java.util.Timer().schedule(
            new java.util.TimerTask()
            {
                @Override
                public void run()
                {
                    final PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
                    final EntityMagmaCube glowingBlockEntity =
                        new EntityMagmaCube(EntityTypes.MAGMA_CUBE, ((CraftWorld) bukkitWorld).getHandle());

                    glowingBlockEntity.setLocation(x + 0.5, y, z + 0.5, 0, 0);
                    glowingBlockEntity.setHeadRotation(0);
                    glowingBlockEntity.setInvisible(true);
                    glowingBlockEntity.setInvulnerable(true);
                    glowingBlockEntity.setNoAI(true);
                    glowingBlockEntity.setSilent(true);
                    glowingBlockEntity.setFlag(6, true); // Glowing
                    glowingBlockEntity.setFlag(5, true); // Invisible
                    glowingBlockEntity.setSize(2, true);
                    teams.get(color).addEntry(glowingBlockEntity.getName());

                    final PacketPlayOutSpawnEntityLiving spawnGlowingBlock =
                        new PacketPlayOutSpawnEntityLiving(glowingBlockEntity);
                    playerConnection.sendPacket(spawnGlowingBlock);

                    final PacketPlayOutEntityMetadata entityMetadata =
                        new PacketPlayOutEntityMetadata(glowingBlockEntity.getId(),
                                                        glowingBlockEntity.getDataWatcher(), false);
                    playerConnection.sendPacket(entityMetadata);

                    new java.util.Timer().schedule(
                        new java.util.TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                final PacketPlayOutEntityDestroy killMagmaCube =
                                    new PacketPlayOutEntityDestroy(glowingBlockEntity.getId());
                                playerConnection.sendPacket(killMagmaCube);
                                cancel();
                            }
                        }, time * 1000);
                    cancel();
                }
            }, 0);
    }
}
