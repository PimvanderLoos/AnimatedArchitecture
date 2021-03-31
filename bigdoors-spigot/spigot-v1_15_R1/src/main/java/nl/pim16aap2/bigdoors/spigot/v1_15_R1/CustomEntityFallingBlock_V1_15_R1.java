package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.Getter;
import net.minecraft.server.v1_15_R1.BlockPosition;
import net.minecraft.server.v1_15_R1.Blocks;
import net.minecraft.server.v1_15_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_15_R1.DataWatcher;
import net.minecraft.server.v1_15_R1.DataWatcherObject;
import net.minecraft.server.v1_15_R1.DataWatcherRegistry;
import net.minecraft.server.v1_15_R1.Entity;
import net.minecraft.server.v1_15_R1.EntityFallingBlock;
import net.minecraft.server.v1_15_R1.EntityTypes;
import net.minecraft.server.v1_15_R1.EnumMoveType;
import net.minecraft.server.v1_15_R1.GameProfileSerializer;
import net.minecraft.server.v1_15_R1.IBlockData;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.PacketPlayOutEntity;
import net.minecraft.server.v1_15_R1.PlayerChunkMap;
import net.minecraft.server.v1_15_R1.TagsBlock;
import net.minecraft.server.v1_15_R1.WorldServer;
import nl.pim16aap2.bigdoors.api.ICustomEntityFallingBlock;
import nl.pim16aap2.bigdoors.util.vector.Vector3DdConst;
import org.bukkit.craftbukkit.v1_15_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jetbrains.annotations.NotNull;

/**
 * V1_15_R1 implementation of {@link ICustomEntityFallingBlock}.
 *
 * @author Pim
 * @see ICustomEntityFallingBlock
 */
public class CustomEntityFallingBlock_V1_15_R1 extends net.minecraft.server.v1_15_R1.EntityFallingBlock
    implements ICustomEntityFallingBlock
{
    @NotNull
    protected static final DataWatcherObject<BlockPosition> d = DataWatcher.a(EntityFallingBlock.class,
                                                                              DataWatcherRegistry.l);
    public int ticksLived;
    public boolean dropItem;
    public boolean hurtEntities;
    public NBTTagCompound tileEntityData;
    private IBlockData block;
    private boolean f;
    private int fallHurtMax;
    private float fallHurtAmount;
    private final org.bukkit.World bukkitWorld;
    private boolean g;
    private PlayerChunkMap.EntityTracker tracker;
    private final WorldServer worldServer;

    @Getter
    private Vector3DdConst previousPosition;
    @Getter
    private Vector3DdConst currentPosition;
    @Getter
    private Vector3DdConst futurePosition;

    public CustomEntityFallingBlock_V1_15_R1(final @NotNull org.bukkit.World world, final double d0, final double d1,
                                             final double d2, final @NotNull IBlockData iblockdata)
        throws Exception
    {
        super(EntityTypes.FALLING_BLOCK, ((CraftWorld) world).getHandle());
        bukkitWorld = world;
        worldServer = ((CraftWorld) bukkitWorld).getHandle();
        block = iblockdata;
        i = true;
        setPosition(d0, d1 + (1.0F - getHeight()) / 2.0F, d2);
        dropItem = false;
        setNoGravity(true);
        fallHurtMax = 0;
        fallHurtAmount = 0.0F;
        setMot(0, 0, 0);
        lastX = d0;
        lastY = d1;
        lastZ = d2;

        previousPosition = new Vector3DdConst(d0, d1, d2);
        currentPosition = previousPosition;
        futurePosition = currentPosition;

        // try setting noclip twice, because it doesn't seem to stick.
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

    private void cyclePositions(@NotNull Vector3DdConst newPosition)
    {
        previousPosition = currentPosition;
        currentPosition = futurePosition;
        futurePosition = newPosition;
    }

    public boolean teleport(final @NotNull Vector3DdConst newPosition, final @NotNull Vector3DdConst rotation)
    {
        final double distance = futurePosition.getDistance(newPosition);
        cyclePositions(newPosition);

        double deltaX = currentPosition.getX() - previousPosition.getX();
        double deltaY = currentPosition.getY() - previousPosition.getY();
        double deltaZ = currentPosition.getZ() - previousPosition.getZ();

        short relX = (short) (deltaX * 4096);
        short relY = (short) (deltaY * 4096);
        short relZ = (short) (deltaZ * 4096);

        PacketPlayOutEntity.PacketPlayOutRelEntityMove tppacket =
            new PacketPlayOutEntity.PacketPlayOutRelEntityMove(getId(), relX, relY, relZ, true);

        tracker.broadcast(tppacket);
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
            double motY = getMot().y * 1.0D;
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
    protected void b(final @NotNull NBTTagCompound nbttagcompound)
    {
        nbttagcompound.set("BlockState", GameProfileSerializer.a(block));
        nbttagcompound.setInt("Time", ticksLived);
        nbttagcompound.setBoolean("DropItem", dropItem);
        nbttagcompound.setBoolean("HurtEntities", hurtEntities);
        nbttagcompound.setFloat("FallHurtAmount", fallHurtAmount);
        nbttagcompound.setInt("FallHurtMax", fallHurtMax);
        if (tileEntityData != null)
            nbttagcompound.set("TileEntityData", tileEntityData);

    }

    @Override
    protected void a(final @NotNull NBTTagCompound nbttagcompound)
    {
        block = GameProfileSerializer.d(nbttagcompound.getCompound("BlockState"));
        ticksLived = nbttagcompound.getInt("Time");
        if (nbttagcompound.hasKeyOfType("HurtEntities", 99))
        {
            hurtEntities = nbttagcompound.getBoolean("HurtEntities");
            fallHurtAmount = nbttagcompound.getFloat("FallHurtAmount");
            fallHurtMax = nbttagcompound.getInt("FallHurtMax");
        }
        else if (block.a(TagsBlock.ANVIL))
            hurtEntities = true;

        if (nbttagcompound.hasKeyOfType("DropItem", 99))
            dropItem = nbttagcompound.getBoolean("DropItem");

        if (nbttagcompound.hasKeyOfType("TileEntityData", 10))
            tileEntityData = nbttagcompound.getCompound("TileEntityData");

        if (block.isAir())
            block = Blocks.SAND.getBlockData();

    }

    @Override
    public void a(final boolean flag)
    {
        hurtEntities = flag;
    }

    @Override
    public void appendEntityCrashDetails(final @NotNull CrashReportSystemDetails crashreportsystemdetails)
    {
        super.appendEntityCrashDetails(crashreportsystemdetails);
        crashreportsystemdetails.a("Imitating BlockState", block.toString());
    }

    @Override
    public @NotNull IBlockData getBlock()
    {
        return block;
    }
}
