package nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1;

import net.minecraft.server.v1_14_R1.EntityMagmaCube;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_14_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_14_R1.PlayerConnection;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.util.IRestartableHolder;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.Restartable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * V1_14_R1 implementation of {@link IGlowingBlockSpawner}.
 *
 * @author Pim
 * @see IGlowingBlockSpawner
 */
public class GlowingBlockSpawner_V1_14_R1 extends Restartable implements IGlowingBlockSpawner
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
    public GlowingBlockSpawner_V1_14_R1(final @NotNull IRestartableHolder holder, final @NotNull PLogger plogger)
    {
        super(holder);
        this.plogger = plogger;
        teams = new HashMap<>();
        scoreboard = org.bukkit.Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        init();
    }

    /**
     * Initializes all teams.
     */
    private void init()
    {
        for (ChatColor col : ChatColor.values())
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
        String name = "BigDoors" + color.ordinal();
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
    public void spawnGlowinBlock(final @NotNull UUID playerUUID, final @NotNull String world, final long time,
                                 final double x, final double y, final double z)
    {
        spawnGlowinBlock(playerUUID, world, time, x, y, z, ChatColor.WHITE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void spawnGlowinBlock(final @NotNull UUID playerUUID, final @NotNull String world, final long time,
                                 final double x, final double y, final double z, final @NotNull Object colorObject)
    {
        org.bukkit.ChatColor color;
        if (!(colorObject instanceof ChatColor))
        {
            plogger.logException(new IllegalArgumentException("Color object not a ChatColor!"));
            return;
        }

        color = (org.bukkit.ChatColor) colorObject;
        if (!teams.containsKey(color))
        {
            plogger.logException(new IllegalArgumentException("Unsupported color: " + color.name()));
            return;
        }

        Player player = Bukkit.getPlayer(playerUUID);
        World bukkitWorld = Bukkit.getWorld(world);
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
                    PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
                    EntityMagmaCube magmaCube = new EntityMagmaCube(EntityTypes.MAGMA_CUBE,
                                                                    ((CraftWorld) bukkitWorld).getHandle());
                    magmaCube.setLocation(x + 0.5, y, z + 0.5, 0, 0);
                    magmaCube.setSize(2, true);
                    magmaCube.setFlag(6, true); //Glowing
                    magmaCube.setNoGravity(true);
                    magmaCube.setInvisible(true);
                    magmaCube.setNoAI(true);
                    magmaCube.setSilent(true);
                    magmaCube.setInvulnerable(true);
                    magmaCube.setHeadRotation(0);
                    magmaCube.collides = false;
                    teams.get(color).addEntry(magmaCube.getName());

                    PacketPlayOutSpawnEntityLiving spawnMagmaCube = new PacketPlayOutSpawnEntityLiving(magmaCube);
                    connection.sendPacket(spawnMagmaCube);

                    new java.util.Timer().schedule(
                        new java.util.TimerTask()
                        {
                            @Override
                            public void run()
                            {
                                PacketPlayOutEntityDestroy killMagmaCube = new PacketPlayOutEntityDestroy(
                                    magmaCube.getId());
                                connection.sendPacket(killMagmaCube);
                                cancel();
                            }
                        }, time * 1000);
                    cancel();
                }
            }, 0
        );
    }
}
