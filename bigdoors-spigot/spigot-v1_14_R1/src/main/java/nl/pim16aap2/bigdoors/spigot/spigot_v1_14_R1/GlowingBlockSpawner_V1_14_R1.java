package nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1;

import net.minecraft.server.v1_14_R1.EntityShulker;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_14_R1.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_14_R1.PlayerConnection;
import nl.pim16aap2.bigdoors.api.IGlowingBlockSpawner;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * V1_14_R1 implementation of {@link IGlowingBlockSpawner}.
 *
 * @author Pim
 * @see IGlowingBlockSpawner
 */
public class GlowingBlockSpawner_V1_14_R1 implements IGlowingBlockSpawner
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void spawnGlowinBlock(@NotNull UUID playerUUID, @NotNull String world, long time, double x, double y,
                                 double z)
    {
        Player player = Bukkit.getPlayer(playerUUID);
        World bukkitWorld = Bukkit.getWorld(world);

        new java.util.Timer().schedule(
                new java.util.TimerTask()
                {
                    @Override
                    public void run()
                    {
                        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;
                        EntityShulker shulker = new EntityShulker(EntityTypes.SHULKER,
                                                                  ((CraftWorld) bukkitWorld).getHandle());
                        shulker.setLocation(x, y, z, 0, 0);
                        shulker.setFlag(6, true); //Glow
                        shulker.setNoGravity(true);
                        shulker.setInvisible(true);
                        shulker.setNoAI(true);
                        shulker.setSilent(true);

                        PacketPlayOutSpawnEntityLiving spawnShulker = new PacketPlayOutSpawnEntityLiving(shulker);
                        connection.sendPacket(spawnShulker);

                        new java.util.Timer().schedule(
                                new java.util.TimerTask()
                                {
                                    @Override
                                    public void run()
                                    {
                                        PacketPlayOutEntityDestroy killShulker = new PacketPlayOutEntityDestroy(
                                                shulker.getId());
                                        connection.sendPacket(killShulker);
                                        cancel();
                                    }
                                }, time * 1000);
                        cancel();
                    }
                }, 0
        );
    }
}
