package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import net.minecraft.server.v1_15_R1.EntityMagmaCube;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_15_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_15_R1.PlayerConnection;
import nl.pim16aap2.bigdoors.api.IGlowingBlock;
import nl.pim16aap2.bigdoors.api.PColor;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * V1_15_R1 implementation of {@link IGlowingBlock}.
 *
 * @author Pim
 * @see IGlowingBlock
 */
public class GlowingBlock_V1_15_R1 implements IGlowingBlock
{
    @Nullable
    private Player player;

    private int entityID;

    private boolean alive = false;

    GlowingBlock_V1_15_R1(final @NotNull Player player, final @NotNull World bukkitWorld, final @NotNull PColor color,
                          final double x, final double y, final double z)
    {
        final @Nullable GlowingBlockSpawner_V1_15_R1 spawner = GlowingBlockSpawner_V1_15_R1.get();
        if (spawner == null)
            return;

        this.player = player;

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
        spawner.getTeams().get(color).addEntry(glowingBlockEntity.getName());

        final PacketPlayOutSpawnEntityLiving spawnGlowingBlock =
            new PacketPlayOutSpawnEntityLiving(glowingBlockEntity);
        playerConnection.sendPacket(spawnGlowingBlock);

        final PacketPlayOutEntityMetadata entityMetadata =
            new PacketPlayOutEntityMetadata(glowingBlockEntity.getId(),
                                            glowingBlockEntity.getDataWatcher(), false);
        playerConnection.sendPacket(entityMetadata);
        alive = true;
        entityID = glowingBlockEntity.getId();
    }

    @Override
    public void kill()
    {
        if (!alive || player == null)
            return;

        final PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
        final PacketPlayOutEntityDestroy killMagmaCube = new PacketPlayOutEntityDestroy(entityID);

        playerConnection.sendPacket(killMagmaCube);

        alive = false;
    }
}
