package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import net.minecraft.server.v1_15_R1.EntityMagmaCube;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.PacketDataSerializer;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_15_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_15_R1.PlayerConnection;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.api.restartable.IRestartableHolder;
import nl.pim16aap2.bigdoors.spigot.util.GlowingBlockSpawner;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import nl.pim16aap2.bigdoors.util.IGlowingBlock;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.TimerTask;
import java.util.logging.Level;

/**
 * V1_15_R1 implementation of {@link IGlowingBlock}.
 *
 * @author Pim
 * @see IGlowingBlock
 */
public class GlowingBlock_V1_15_R1 implements IGlowingBlock
{
    @NotNull
    private final World world;

    private @Nullable TimerTask killTask;

    private Integer entityID = null;

    private boolean alive = false;

    private final @NotNull Map<PColor, Team> teams;
    private final @NotNull Player player;
    private final @NotNull IRestartableHolder restartableHolder;

    public GlowingBlock_V1_15_R1(final @NotNull Player player, final @NotNull World world,
                                 final @NotNull Map<PColor, Team> teams,
                                 final @NotNull IRestartableHolder restartableHolder)
    {
        this.player = player;
        this.world = world;
        this.teams = teams;
        this.restartableHolder = restartableHolder;
    }


    private @NotNull Optional<PlayerConnection> getConnection()
    {
        final @Nullable EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        if (entityPlayer == null)
        {
            BigDoors.get().getPLogger().logMessage(Level.WARNING, "NMS entity of player: " + player.getDisplayName() +
                " could not be found! They cannot receive Glowing Block packets!");
            return Optional.empty();
        }
        return Optional.ofNullable(entityPlayer.playerConnection);
    }

    @Override
    public void kill()
    {
        killTask = null;
        restartableHolder.deregisterRestartable(this);
        if (!alive)
            return;

        getConnection().ifPresent(connection -> connection.sendPacket(new PacketPlayOutEntityDestroy(entityID)));
        alive = false;
    }

    @Override
    public void teleport(final @NotNull Vector3DdConst position)
    {
        if (!alive)
            return;
        getConnection().ifPresent(connection -> connection
            .sendPacket(new PacketPlayOutGlowingBlockTeleport(position.getX(), position.getY(), position.getZ())));
    }

    @Override
    public void spawn(final @NotNull PColor pColor, final double x, final double y, final double z, final long ticks)
    {
        final @NotNull Optional<PlayerConnection> playerConnectionOpt = getConnection();
        if (playerConnectionOpt.isEmpty())
            return;

        if (killTask != null)
            killTask.cancel();

        final PlayerConnection playerConnection = playerConnectionOpt.get();
        final EntityMagmaCube glowingBlockEntity =
            new EntityMagmaCube(EntityTypes.MAGMA_CUBE, ((CraftWorld) world).getHandle());
        entityID = entityID == null ? glowingBlockEntity.getId() : entityID;

        glowingBlockEntity.setLocation(x + 0.5, y, z + 0.5, 0, 0);
        glowingBlockEntity.setHeadRotation(0);
        glowingBlockEntity.setInvisible(true);
        glowingBlockEntity.setInvulnerable(true);
        glowingBlockEntity.setNoAI(true);
        glowingBlockEntity.setSilent(true);
        glowingBlockEntity.setFlag(6, true); // Glowing
        glowingBlockEntity.setFlag(5, true); // Invisible
        glowingBlockEntity.setSize(2, true);
        teams.get(pColor).addEntry(glowingBlockEntity.getName());

        final PacketPlayOutSpawnEntityLiving spawnGlowingBlock =
            new PacketPlayOutSpawnEntityLiving(glowingBlockEntity);
        playerConnection.sendPacket(spawnGlowingBlock);

        final PacketPlayOutEntityMetadata entityMetadata =
            new PacketPlayOutEntityMetadata(glowingBlockEntity.getId(),
                                            glowingBlockEntity.getDataWatcher(), false);
        playerConnection.sendPacket(entityMetadata);
        alive = true;
        restartableHolder.registerRestartable(this);

        killTask = new TimerTask()
        {
            @Override
            public void run()
            {
                kill();
            }
        };
        BigDoors.get().getPlatform().getPExecutor().runSyncLater(killTask, ticks);
    }

    @Override
    public void restart()
    {
        shutdown();
    }

    @Override
    public void shutdown()
    {
        if (killTask == null)
            return;
        killTask.run();
    }

    @AllArgsConstructor
    private class PacketPlayOutGlowingBlockTeleport extends PacketPlayOutEntityTeleport
    {
        private final double x, y, z;

        @Override
        public void b(PacketDataSerializer var0)
        {
            var0.d(entityID);
            var0.writeDouble(x);
            var0.writeDouble(y);
            var0.writeDouble(z);
            var0.writeByte(0);
            var0.writeByte(0);
            var0.writeBoolean(false);
        }
    }

    public static class Factory implements IGlowingBlockFactory
    {
        @Override
        public @NotNull Optional<IGlowingBlock> createGlowingBlock(final @NotNull Player player,
                                                                   final @NotNull World world,
                                                                   final @NotNull IRestartableHolder restartableHolder)
        {
            @NonNull Optional<IGlowingBlockSpawner> spawnerOpt = BigDoors.get().getPlatform().getGlowingBlockSpawner();
            if (spawnerOpt.isEmpty() || !(spawnerOpt.get() instanceof GlowingBlockSpawner))
                return Optional.empty();

            return Optional.of(new GlowingBlock_V1_15_R1(player, world,
                                                         ((GlowingBlockSpawner) spawnerOpt.get()).getTeams(),
                                                         restartableHolder));
        }
    }
}
