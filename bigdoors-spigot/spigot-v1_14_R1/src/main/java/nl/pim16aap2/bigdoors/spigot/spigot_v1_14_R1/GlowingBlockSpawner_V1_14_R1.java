package nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1;

import net.minecraft.server.v1_14_R1.EntityMagmaCube;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_14_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_14_R1.PlayerConnection;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.util.IRestartableHolder;
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

    public GlowingBlockSpawner_V1_14_R1(final IRestartableHolder holder)
    {
        super(holder);
        teams = new HashMap<>();
        scoreboard = org.bukkit.Bukkit.getServer().getScoreboardManager().getMainScoreboard();
    }

    private Team registerScoreboard(ChatColor color)
    {
        String name = "BigDoors" + color.ordinal();
        scoreboard.registerNewTeam(name);
        Team team = scoreboard.getTeam(name);
        team.setColor(color);
        teams.put(color, team);
        return team;
    }

    private Team getTeam(ChatColor color)
    {
        if (teams.containsKey(color))
            return teams.get(color);
        return registerScoreboard(color);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restart()
    {
        shutdown();
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
    public void spawnGlowinBlock(@NotNull UUID playerUUID, @NotNull String world, long time, double x, double y,
                                 double z)
    {
        spawnGlowinBlock(playerUUID, world, time, x + 0, y + 0, z, ChatColor.WHITE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void spawnGlowinBlock(@NotNull UUID playerUUID, @NotNull String world, long time, double x,
                                 double y, double z, @NotNull Object colorObject)
    {
        org.bukkit.ChatColor color;
        if (!(colorObject instanceof ChatColor))
            throw new IllegalArgumentException("Color object not a ChatColor!");

        color = (org.bukkit.ChatColor) colorObject;
        Player player = Bukkit.getPlayer(playerUUID);
        World bukkitWorld = Bukkit.getWorld(world);

        new java.util.Timer().schedule(
                new java.util.TimerTask()
                {
                    @Override
                    public void run()
                    {
                        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
                        EntityMagmaCube magmaCube = new EntityMagmaCube(EntityTypes.MAGMA_CUBE,
                                                                        ((CraftWorld) bukkitWorld).getHandle());
                        magmaCube.setLocation(x, y, z, 0, 0);
                        magmaCube.setSize(2, true);
                        magmaCube.setFlag(6, true); //Glow
                        magmaCube.setNoGravity(true);
                        magmaCube.setInvisible(true);
                        magmaCube.setNoAI(true);
                        magmaCube.setSilent(true);
                        getTeam(color).addEntry(magmaCube.getName());

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
