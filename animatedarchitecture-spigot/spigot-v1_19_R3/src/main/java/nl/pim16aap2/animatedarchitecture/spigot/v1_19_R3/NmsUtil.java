package nl.pim16aap2.animatedarchitecture.spigot.v1_19_R3;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketDataSerializer;
import net.minecraft.network.protocol.game.PacketPlayOutEntityTeleport;
import nl.pim16aap2.animatedarchitecture.core.util.vector.IVector3D;

public final class NmsUtil
{
    private NmsUtil()
    {
    }

    public static PacketPlayOutEntityTeleport newPacketPlayOutEntityTeleport(
        int entityId, IVector3D to, IVector3D rotation)
    {
        // int + 3 * double + 2 * byte + 1 * boolean = 4 + 3 * 8 + 2 + 1 = 31 bytes
        final PacketDataSerializer dataSerializer = new PacketDataSerializer(Unpooled.directBuffer(31));

        dataSerializer.d(entityId);
        dataSerializer.writeDouble(to.xD());
        dataSerializer.writeDouble(to.yD());
        dataSerializer.writeDouble(to.zD());
        dataSerializer.writeByte((byte) ((int) (rotation.yD() * 256.0F / 360.0F)));
        dataSerializer.writeByte((byte) ((int) (rotation.xD() * 256.0F / 360.0F)));
        dataSerializer.writeBoolean(false);

        return new PacketPlayOutEntityTeleport(dataSerializer);
    }
}
