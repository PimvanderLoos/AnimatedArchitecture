package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.Blocks;
import net.minecraft.server.v1_15_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.EnumMoveType;
import net.minecraft.server.v1_15_R1.GameProfileSerializer;
import net.minecraft.server.v1_15_R1.IBlockData;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntity;
import net.minecraft.server.v1_15_R1.PlayerChunkMap;
import net.minecraft.server.v1_15_R1.WorldServer;
import nl.pim16aap2.bigdoors.api.ICustomEntityFallingBlock;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jetbrains.annotations.Nullable;

/**
 * V1_15_R1 implementation of {@link ICustomEntityFallingBlock}.
 *
 * @author Pim
 * @see ICustomEntityFallingBlock
 */
@EqualsAndHashCode(callSuper = true)
public class CustomEntityFallingBlock_V1_15_R1 extends net.minecraft.server.v1_15_R1.EntityFallingBlock
    implements ICustomEntityFallingBlock
{
    // ticksLived is also a field in NMS.EntityFallingBlock. However, we want to override that on purpose.
    @SuppressWarnings("squid:S2387")
    @Setter
    private int ticksLived;

    // tileEntityData is also a field in NMS.EntityFallingBlock. However, we want to override that on purpose.
    @SuppressWarnings("squid:S2387")
    private @Nullable NBTTagCompound tileEntityData;

    private IBlockData block;
    private int fallHurtMax;
    private float fallHurtAmount;
    private final org.bukkit.World bukkitWorld;
    private @Nullable PlayerChunkMap.EntityTracker tracker;
    private final WorldServer worldServer;

    @Getter
    private Vector3Dd previousPosition;
    @Getter
    private Vector3Dd currentPosition;
    @Getter
    private Vector3Dd futurePosition;

    public CustomEntityFallingBlock_V1_15_R1(org.bukkit.World world, double d0, double d1, double d2,
                                             IBlockData iblockdata)
        throws Exception
    {
        super(EntityTypes.FALLING_BLOCK, ((CraftWorld) world).getHandle());
        bukkitWorld = world;
        worldServer = ((CraftWorld) bukkitWorld).getHandle();
        block = iblockdata;
        i = true;
        setPosition(d0, d1 + (1.0F - getHeight()) / 2.0F, d2);
        setNoGravity(true);
        fallHurtMax = 0;
        fallHurtAmount = 0.0F;
        setMot(0, 0, 0);
        lastX = d0;
        lastY = d1;
        lastZ = d2;

        previousPosition = new Vector3Dd(d0, d1, d2);
        currentPosition = previousPosition;
        futurePosition = currentPosition;

        // try setting noClip twice, because it doesn't seem to stick.
        noclip = true;
        a(new BlockPosition(this));
        spawn();
        noclip = true;
    }

    @Override
    public void die()
    {
        for (Entity ent : passengers)
            ent.dead = true;
        dead = true;
    }

    public void spawn()
        throws Exception
    {
        ((org.bukkit.craftbukkit.v1_15_R1.CraftWorld) bukkitWorld).getHandle().addEntity(this, SpawnReason.CUSTOM);
        tracker = worldServer.getChunkProvider().playerChunkMap.trackedEntities.get(getId());
        if (tracker == null)
            throw new Exception("Failed to obtain EntityTracker for FallingBlock: " + getId());
    }

    private void cyclePositions(Vector3Dd newPosition)
    {
        previousPosition = currentPosition;
        currentPosition = futurePosition;
        futurePosition = newPosition;
    }

    @SuppressWarnings("unused")
    public boolean teleport(Vector3Dd newPosition, Vector3Dd rotation)
    {
        final double distance = futurePosition.getDistance(newPosition);
        cyclePositions(newPosition);

        double deltaX = currentPosition.x() - previousPosition.x();
        double deltaY = currentPosition.y() - previousPosition.y();
        double deltaZ = currentPosition.z() - previousPosition.z();

        short relX = (short) (deltaX * 4096);
        short relY = (short) (deltaY * 4096);
        short relZ = (short) (deltaZ * 4096);

        PacketPlayOutEntity.PacketPlayOutRelEntityMove tpPacket =
            new PacketPlayOutEntity.PacketPlayOutRelEntityMove(getId(), relX, relY, relZ, true);

        if (tracker != null)
            tracker.broadcast(tpPacket);
        return true;
    }

    @Override
    protected boolean playStepSound()
    {
        return false;
    }

    @Override
    public boolean isInteractable()
    {
        return !dead;
    }

    @Override
    public void tick()
    {
        if (block.isAir())
            die();
        else
        {
            move(EnumMoveType.SELF, getMot());
            double locY = locY();
            if (++ticksLived > 100 && (locY < 1 || locY > 256) || ticksLived > 12000)
                die();

            double motX = getMot().x * 0.9800000190734863D;
            double motY = getMot().y;
            double motZ = getMot().z * 0.9800000190734863D;
            setMot(motX, motY, motZ);
        }
    }

    @Override
    public boolean b(float f, float f1)
    {
        return false;
    }

    @Override
    protected void b(NBTTagCompound nbttagcompound)
    {
        nbttagcompound.set("BlockState", GameProfileSerializer.a(block));
        nbttagcompound.setInt("Time", ticksLived);
        nbttagcompound.setBoolean("DropItem", false);
        nbttagcompound.setBoolean("HurtEntities", false);
        nbttagcompound.setFloat("FallHurtAmount", fallHurtAmount);
        nbttagcompound.setInt("FallHurtMax", fallHurtMax);
        if (tileEntityData != null)
            nbttagcompound.set("TileEntityData", tileEntityData);

    }

    @Override
    protected void a(NBTTagCompound nbttagcompound)
    {
        block = GameProfileSerializer.d(nbttagcompound.getCompound("BlockState"));
        ticksLived = nbttagcompound.getInt("Time");
        if (nbttagcompound.hasKeyOfType("HurtEntities", 99))
        {
            fallHurtAmount = nbttagcompound.getFloat("FallHurtAmount");
            fallHurtMax = nbttagcompound.getInt("FallHurtMax");
        }

        if (nbttagcompound.hasKeyOfType("TileEntityData", 10))
            tileEntityData = nbttagcompound.getCompound("TileEntityData");

        if (block.isAir())
            block = Blocks.SAND.getBlockData();

    }

    @Override
    public void a(boolean flag)
    {
        // ignored
    }

    @Override
    public void appendEntityCrashDetails(CrashReportSystemDetails crashreportsystemdetails)
    {
        super.appendEntityCrashDetails(crashreportsystemdetails);
        crashreportsystemdetails.a("Imitating BlockState", block.toString());
    }

    @Override
    public IBlockData getBlock()
    {
        return block;
    }
}
