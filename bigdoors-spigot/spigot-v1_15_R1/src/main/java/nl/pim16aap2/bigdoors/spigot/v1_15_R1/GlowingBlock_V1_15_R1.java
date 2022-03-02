package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.flogger.Flogger;
import net.minecraft.server.v1_15_R1.EntityMagmaCube;
import net.minecraft.server.v1_15_R1.EntityPlayer;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.PacketDataSerializer;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_15_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_15_R1.PlayerConnection;
import nl.pim16aap2.bigdoors.api.PColor;
import nl.pim16aap2.bigdoors.spigot.util.api.IGlowingBlockFactory;
import nl.pim16aap2.bigdoors.util.IGlowingBlock;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * V1_15_R1 implementation of {@link IGlowingBlock}.
 *
 * @author Pim
 * @see IGlowingBlock
 */
@Flogger
public class GlowingBlock_V1_15_R1 implements IGlowingBlock
{
    private final EntityMagmaCube glowingBlockEntity;
    @Getter
    private final int entityId;

    private boolean alive = false;

    private final Map<PColor, Team> teams;
    private final Player player;

    public GlowingBlock_V1_15_R1(Player player, World world, PColor pColor,
                                 double x, double y, double z, Map<PColor, Team> teams)
    {
        this.player = player;
        this.teams = teams;

        glowingBlockEntity = new EntityMagmaCube(EntityTypes.MAGMA_CUBE, ((CraftWorld) world).getHandle());
        entityId = glowingBlockEntity.getId();
        spawn(pColor, x, y, z);
    }

    private Optional<PlayerConnection> getConnection()
    {
        final @Nullable EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
        if (entityPlayer == null)
        {
            log.at(Level.WARNING)
               .log("NMS entity of player: '%s' could not be found! They cannot receive Glowing Block packets!",
                    player.getDisplayName());
            return Optional.empty();
        }
        return Optional.ofNullable(entityPlayer.playerConnection);
    }

    @Override
    public void kill()
    {
        if (!alive)
            return;

        getConnection().ifPresent(connection -> connection.sendPacket(new PacketPlayOutEntityDestroy(entityId)));
        alive = false;
    }

    @Override
    public void teleport(Vector3Dd position)
    {
        if (!alive)
            return;
        getConnection().ifPresent(connection -> connection
            .sendPacket(new PacketPlayOutGlowingBlockTeleport(position.x(), position.y(), position.z())));
    }

    private void spawn(PColor pColor, double x, double y, double z)
    {
        final @Nullable Team team = teams.get(pColor);
        if (team == null)
        {
            log.at(Level.WARNING)
               .log("Failed to spawn glowing block: Could not find team for color: %s", pColor.name());
            return;
        }

        final Optional<PlayerConnection> playerConnectionOpt = getConnection();
        if (playerConnectionOpt.isEmpty())
            return;

        final PlayerConnection playerConnection = playerConnectionOpt.get();

        glowingBlockEntity.setLocation(x + 0.5, y, z + 0.5, 0, 0);
        glowingBlockEntity.setHeadRotation(0);
        glowingBlockEntity.setInvisible(true);
        glowingBlockEntity.setInvulnerable(true);
        glowingBlockEntity.setNoAI(true);
        glowingBlockEntity.setSilent(true);
        glowingBlockEntity.setFlag(6, true); // Glowing
        glowingBlockEntity.setFlag(5, true); // Invisible
        glowingBlockEntity.setSize(2, true);
        team.addEntry(glowingBlockEntity.getName());

        final PacketPlayOutSpawnEntityLiving spawnGlowingBlock =
            new PacketPlayOutSpawnEntityLiving(glowingBlockEntity);
        playerConnection.sendPacket(spawnGlowingBlock);

        final PacketPlayOutEntityMetadata entityMetadata =
            new PacketPlayOutEntityMetadata(glowingBlockEntity.getId(),
                                            glowingBlockEntity.getDataWatcher(), false);
        playerConnection.sendPacket(entityMetadata);
        alive = true;
    }

    @AllArgsConstructor
    private class PacketPlayOutGlowingBlockTeleport extends PacketPlayOutEntityTeleport
    {
        private final double x;
        private final double y;
        private final double z;

        @Override
        public void b(PacketDataSerializer var0)
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
        public Optional<IGlowingBlock> createGlowingBlock(Player player, World world, PColor pColor,
                                                          double x, double y, double z, Map<PColor, Team> teams)
        {
            try
            {
                final GlowingBlock_V1_15_R1 block = new GlowingBlock_V1_15_R1(player, world, pColor, x, y, z, teams);
                return block.alive ? Optional.of(block) : Optional.empty();
            }
            catch (Exception | ExceptionInInitializerError e)
            {
                log.at(Level.SEVERE).withCause(e)
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
        return obj instanceof GlowingBlock_V1_15_R1 other && this.entityId == other.entityId;
    }
}
