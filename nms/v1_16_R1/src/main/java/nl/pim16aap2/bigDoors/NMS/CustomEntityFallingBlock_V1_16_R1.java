package nl.pim16aap2.bigDoors.NMS;

import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.Blocks;
import net.minecraft.server.v1_16_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_16_R1.DataWatcher;
import net.minecraft.server.v1_16_R1.DataWatcherObject;
import net.minecraft.server.v1_16_R1.DataWatcherRegistry;
import net.minecraft.server.v1_16_R1.Entity;
import net.minecraft.server.v1_16_R1.EntityFallingBlock;
import net.minecraft.server.v1_16_R1.EntityTypes;
import net.minecraft.server.v1_16_R1.EnumMoveType;
import net.minecraft.server.v1_16_R1.GameProfileSerializer;
import net.minecraft.server.v1_16_R1.IBlockData;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.TagsBlock;

/**
 * v1_16_R1 implementation of {@link CustomEntityFallingBlock}.
 *
 * @author Pim
 * @see CustomEntityFallingBlock
 */
public class CustomEntityFallingBlock_V1_16_R1 extends EntityFallingBlock implements CustomEntityFallingBlock
{
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
    private org.bukkit.World bukkitWorld;
    private boolean g;

    public CustomEntityFallingBlock_V1_16_R1(final org.bukkit.World world, final double d0, final double d1,
        final double d2, final IBlockData iblockdata)
    {
        super(EntityTypes.FALLING_BLOCK, ((CraftWorld) world).getHandle());
        bukkitWorld = world;
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

        // try setting noclip twice, because it doesn't seem to stick.
        noclip = true;
        a(new BlockPosition(this.locX(), this.locY(), this.locZ()));
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
    {
        ((org.bukkit.craftbukkit.v1_16_R1.CraftWorld) bukkitWorld).getHandle().addEntity(this, SpawnReason.CUSTOM);
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
    protected void saveData(final NBTTagCompound nbttagcompound)
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
    protected void loadData(final NBTTagCompound nbttagcompound)
    {
        block = GameProfileSerializer.c(nbttagcompound.getCompound("BlockState"));
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
    public void appendEntityCrashDetails(final CrashReportSystemDetails crashreportsystemdetails)
    {
        super.appendEntityCrashDetails(crashreportsystemdetails);
        crashreportsystemdetails.a("Animated BigDoors block with state: ", block.toString());
    }

    @Override
    public IBlockData getBlock()
    {
        return block;
    }
}
