package nl.pim16aap2.bigdoors.spigot.v1_19_R2;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.EntityMagmaCube;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import nl.pim16aap2.bigdoors.util.IGlowingBlock;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * v1_19_R2 implementation of {@link IGlowingBlock}.
 *
 * @author Pim
 * @see IGlowingBlock
 */
@Flogger
public class GlowingBlock_V1_19_R2 implements IGlowingBlock
{
    private final EntityMagmaCube glowingBlockEntity;
    @Getter
    private final int entityId;

    private final AtomicBoolean alive = new AtomicBoolean(false);

    private final Map<PColor, Team> teams;
    private final Player player;

    public GlowingBlock_V1_19_R2(
        Player player, World world, PColor pColor, double x, double y, double z, Map<PColor, Team> teams)
    {
        this.player = player;
        this.teams = teams;

        glowingBlockEntity = new EntityMagmaCube(EntityTypes.ab, ((CraftWorld) world).getHandle());
        entityId = glowingBlockEntity.ah();
        spawn(pColor, x, y, z);
    }

    private Optional<PlayerConnection> getConnection()
    {
        final @Nullable EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        if (entityPlayer == null)
        {
            log.atWarning()
               .log("NMS entity of player: '%s' could not be found! They cannot receive Glowing Block packets!",
                    player.getDisplayName());
            return Optional.empty();
        }
        return Optional.ofNullable(entityPlayer.b);
    }

    @Override
    public void kill()
    {
        if (!alive.getAndSet(false))
            return;

        getConnection().ifPresent(connection -> connection.a(new PacketPlayOutEntityDestroy(entityId)));
    }

    @Override
    public void teleport(Vector3Dd position)
    {
        if (!alive.get())
            return;
        getConnection().ifPresent(connection -> connection.a(new PacketPlayOutGlowingBlockTeleport(position)));
    }

    private void spawn(PColor pColor, double x, double y, double z)
    {
        final @Nullable Team team = teams.get(pColor);
        if (team == null)
        {
            log.atWarning()
               .log("Failed to spawn glowing block: Could not find team for color: %s", pColor.name());
            return;
        }

        final Optional<PlayerConnection> playerConnectionOpt = getConnection();
        if (playerConnectionOpt.isEmpty())
            return;

        final PlayerConnection playerConnection = playerConnectionOpt.get();

        glowingBlockEntity.a(x, y, z, 0, 0); // setLocation
        glowingBlockEntity.aZ = 0f; // yHeadRot (net.minecraft.world.entity.LivingEntity)
        glowingBlockEntity.j(true); // setInvisible()
        glowingBlockEntity.m(true); // setInvulnerable()
        glowingBlockEntity.s(true); // setNoAi() (net.minecraft.world.entity.Mob)
        glowingBlockEntity.d(true); // setSilent()
        glowingBlockEntity.b(6, true); // setSharedFlag(), tag: Glowing
        glowingBlockEntity.b(5, true); // setSharedFlag(), tag: Invisible
        glowingBlockEntity.a(2, true); // setSize()
        team.addEntry(glowingBlockEntity.ct()); // getStringUUID()

        final PacketPlayOutSpawnEntity spawnGlowingBlock = new PacketPlayOutSpawnEntity(glowingBlockEntity);
        playerConnection.a(spawnGlowingBlock);

        final PacketPlayOutEntityMetadata entityMetadata =
            new PacketPlayOutEntityMetadata(glowingBlockEntity.ah(), glowingBlockEntity.al().c());
        playerConnection.a(entityMetadata);
        alive.set(true);
    }

    private class PacketPlayOutGlowingBlockTeleport extends PacketPlayOutEntityTeleport
    {
        private final double x;
        private final double y;
        private final double z;

        public PacketPlayOutGlowingBlockTeleport(Vector3Dd position)
        {
            super(glowingBlockEntity);
            this.x = position.x();
            this.y = position.y();
            this.z = position.z();
        }

        @Override
        public void a(PacketDataSerializer var0)
        {
            var0.d(entityId);
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
        public Optional<IGlowingBlock> createGlowingBlock(
            Player player, World world, PColor pColor,
            double x, double y, double z, Map<PColor, Team> teams)
        {
            try
            {
                final GlowingBlock_V1_19_R2 block = new GlowingBlock_V1_19_R2(player, world, pColor, x, y, z, teams);
                return block.alive.get() ? Optional.of(block) : Optional.empty();
            }
            catch (Exception | ExceptionInInitializerError e)
            {
                log.atSevere().withCause(e)
                   .log("Failed to spawn glowing block for player %s in world %s.", player, world);
                return Optional.empty();
            }
        }
    }

    @Override
    public int hashCode()
    {
        return entityId;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof GlowingBlock_V1_19_R2 other && this.entityId == other.entityId;
    }
}
