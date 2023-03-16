package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import lombok.Getter;
import lombok.extern.flogger.Flogger;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.EntityMagmaCube;
import nl.pim16aap2.animatedarchitecture.core.api.Color;
import nl.pim16aap2.animatedarchitecture.core.moveblocks.RotatedPosition;
import nl.pim16aap2.animatedarchitecture.core.util.IGlowingBlock;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftMagmaCube;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * v1_19_R3 implementation of {@link IGlowingBlock}.
 *
 * @author Pim
 * @see IGlowingBlock
 */
@Flogger
public class GlowingBlock implements IGlowingBlock
{
    private final EntityMagmaCube glowingBlockEntity;
    @Getter
    private final int entityId;

    private final AtomicBoolean alive = new AtomicBoolean(false);

    private final Map<Color, Team> teams;
    private final Player player;

    public GlowingBlock(
        Player player, World world, Color pColor, RotatedPosition rotatedPosition, Map<Color, Team> teams)
    {
        this.player = player;
        this.teams = teams;

        glowingBlockEntity = new EntityMagmaCube(EntityTypes.al, ((CraftWorld) world).getHandle());
        entityId = glowingBlockEntity.af();
        spawn(pColor, rotatedPosition);
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
    public void teleport(RotatedPosition rotatedPosition)
    {
        if (!alive.get())
            return;
        getConnection().ifPresent(
            connection -> connection.a(NmsUtil.newPacketPlayOutEntityTeleport(
                entityId, rotatedPosition.position(), rotatedPosition.rotation())));
    }

    private void spawn(Color pColor, RotatedPosition rotatedPosition)
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

        final CraftMagmaCube wrapper = new CraftMagmaCube((CraftServer) Bukkit.getServer(), glowingBlockEntity);

        final PlayerConnection playerConnection = playerConnectionOpt.get();

        glowingBlockEntity.a(
            rotatedPosition.position().x(), rotatedPosition.position().y(), rotatedPosition.position().z(), 0, 0);
        wrapper.setGlowing(true);
        wrapper.setSilent(true);
        wrapper.setInvulnerable(true);
        wrapper.setSize(2);
        wrapper.setInvisible(true);
        wrapper.setAI(false);

        glowingBlockEntity.aV = 0f; // yHeadRot (net.minecraft.world.entity.LivingEntity)
        team.addEntry(glowingBlockEntity.ct()); // getStringUUID()

        wrapper.setRotation((float) rotatedPosition.yaw(), (float) rotatedPosition.pitch());

        final PacketPlayOutSpawnEntity spawnGlowingBlock = new PacketPlayOutSpawnEntity(glowingBlockEntity);
        playerConnection.a(spawnGlowingBlock);

        final PacketPlayOutEntityMetadata entityMetadata =
            new PacketPlayOutEntityMetadata(glowingBlockEntity.af(), glowingBlockEntity.aj().c());
        playerConnection.a(entityMetadata);
        alive.set(true);
    }

    @Override
    public int hashCode()
    {
        return entityId;
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof GlowingBlock other && this.entityId == other.entityId;
    }
}
