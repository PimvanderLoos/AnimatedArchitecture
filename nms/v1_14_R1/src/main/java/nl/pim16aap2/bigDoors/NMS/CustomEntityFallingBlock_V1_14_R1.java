package nl.pim16aap2.bigDoors.NMS;

import java.util.Iterator;
import java.util.List;

import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.google.common.collect.Lists;

import net.minecraft.server.v1_14_R1.AxisAlignedBB;
import net.minecraft.server.v1_14_R1.Block;
import net.minecraft.server.v1_14_R1.BlockConcretePowder;
import net.minecraft.server.v1_14_R1.BlockFalling;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.Blocks;
import net.minecraft.server.v1_14_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_14_R1.DataWatcher;
import net.minecraft.server.v1_14_R1.DataWatcherObject;
import net.minecraft.server.v1_14_R1.DataWatcherRegistry;
import net.minecraft.server.v1_14_R1.Entity;
import net.minecraft.server.v1_14_R1.EntityFallingBlock;
import net.minecraft.server.v1_14_R1.EntityTypes;
import net.minecraft.server.v1_14_R1.EnumDirection;
import net.minecraft.server.v1_14_R1.EnumMoveType;
import net.minecraft.server.v1_14_R1.GameProfileSerializer;
import net.minecraft.server.v1_14_R1.IBlockData;
import net.minecraft.server.v1_14_R1.MovingObjectPosition;
import net.minecraft.server.v1_14_R1.MovingObjectPositionBlock;
import net.minecraft.server.v1_14_R1.NBTTagCompound;
import net.minecraft.server.v1_14_R1.RayTrace;
import net.minecraft.server.v1_14_R1.TagsBlock;
import net.minecraft.server.v1_14_R1.TagsFluid;
import net.minecraft.server.v1_14_R1.Vec3D;

/*
 * This is a custom falling block entity.
 * Changes:
 * - It does not die after a specified (600) amount of ticks.
 * - Easy kill() function to kill it.
 * - Not affected by gravity.
 * - Does not hurt entities (by default, anyway).
 * - NoClip enabled
 */

@SuppressWarnings("hiding")
public class CustomEntityFallingBlock_V1_14_R1 extends net.minecraft.server.v1_14_R1.EntityFallingBlock implements CustomEntityFallingBlock
{
    private IBlockData block;
    public int ticksLived;
    public boolean dropItem;
    private boolean f;
    public boolean hurtEntities;
    private int fallHurtMax;
    private float fallHurtAmount;
    public NBTTagCompound tileEntityData;
    protected static final DataWatcherObject<BlockPosition> d = DataWatcher.a(EntityFallingBlock.class, DataWatcherRegistry.l);
    private org.bukkit.World bukkitWorld;

    public CustomEntityFallingBlock_V1_14_R1(org.bukkit.World world, double d0, double d1, double d2, IBlockData iblockdata)
    {
        super(EntityTypes.FALLING_BLOCK, ((CraftWorld) world).getHandle());
        bukkitWorld = world;
        block = iblockdata;
        i = true;
        setPosition(d0, d1 + (1.0F - getHeight()) / 2.0F, d2);
        dropItem = false;
        noclip = true;
        setNoGravity(true);
        fallHurtMax = 0;
        fallHurtAmount = 0.0F;
        this.setMot(0, 0, 0);
        lastX = d0;
        lastY = d1;
        lastZ = d2;
        this.a(new BlockPosition(this));
        spawn();
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
        ((org.bukkit.craftbukkit.v1_14_R1.CraftWorld) bukkitWorld).getHandle().addEntity(this, SpawnReason.CUSTOM);
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

    @SuppressWarnings("unused")
    private List<Entity> getFallingBlocksOnSide(AxisAlignedBB bb, EnumDirection dir)
    {
        /** AxisAlignedBB:
         *  a/d = min/max x
         *  b/e = min/max y
         *  c/f = min/max z
        **/
        double minX = bb.minX;
        double minY = bb.minY;
        double minZ = bb.minZ;
        double maxX = bb.maxX;
        double maxY = bb.maxY;
        double maxZ = bb.maxZ;
        switch(dir)
        {
        case DOWN:
            minY -= 1;
            maxY -= 1;
            break;

        case UP:
            minY += 1;
            maxY += 1;
            break;

        case NORTH:
            minZ -= 1;
            maxZ -= 1;
            break;

        case SOUTH:
            minZ += 1;
            maxZ += 1;
            break;

        case WEST:
            minX -= 1;
            maxX -= 1;
            break;

        case EAST:
            minX += 1;
            maxX += 1;
        }

        AxisAlignedBB newBB = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
        List<Entity> list   = world.getEntities(this, newBB);
        List<Entity> ret    = Lists.newArrayList();
        if (!list.isEmpty())
        {
            Iterator<Entity> iterator = list.iterator();
            while (iterator.hasNext())
            {
                Entity entity = iterator.next();
                if (entity instanceof CustomEntityFallingBlock_V1_14_R1)
                    ret.add(entity);
            }
        }
        return ret;
    }

    @Override
    public void tick()
    {
        if (block.isAir())
            die();
        else
        {
            lastX = locX;
            lastY = locY;
            lastZ = locZ;
            Block block = this.block.getBlock();
            BlockPosition blockposition;

            if (ticksLived++ == 0)
            {
                blockposition = new BlockPosition(this);
                if (world.getType(blockposition).getBlock() == block)
                    world.a(blockposition, false);
            }

            // If gravity (not no gravity), DO NOT apply negative y speed.
            if (!isNoGravity())
                this.setMot(getMot().add(0.0D, -0.04D, 0.0D));

            move(EnumMoveType.SELF, getMot());
            if (!world.isClientSide)
            {
                blockposition = new BlockPosition(this);
                boolean isConcretePowder = this.block.getBlock() instanceof BlockConcretePowder;
                // TODO: Look into this. Is this really needed? Might it interfere??
                boolean flag1 = isConcretePowder && world.getFluid(blockposition).a(TagsFluid.WATER); // Concrete in powder?
                Vec3D mot = getMot();
                double d0 = mot.x * mot.x + mot.y * mot.y + mot.z * mot.z;

                if (isConcretePowder && d0 > 1.0D)
                {
                    MovingObjectPositionBlock movingobjectpositionblock = world.rayTrace(new RayTrace(new Vec3D(lastX, lastY, lastZ), new Vec3D(locX, locY, locZ), RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.SOURCE_ONLY, this));

                    if (movingobjectpositionblock.getType() != MovingObjectPosition.EnumMovingObjectType.MISS && world.getFluid(movingobjectpositionblock.getBlockPosition()).a(TagsFluid.WATER))
                    {
                        blockposition = movingobjectpositionblock.getBlockPosition();
                        flag1 = true;
                    }
                }

                if (!onGround && !flag1)
                {
//                    if (this.ticksLived > 100 && !this.world.isClientSide && (blockposition.getY() < 1 || blockposition.getY() > 256) || this.ticksLived > 600)
                    // PIM: Changed to make them live longer (12k ticks instead of 600 -> 10min instead of .5min).
                    if (ticksLived > 100 && !world.isClientSide && (blockposition.getY() < 1 || blockposition.getY() > 256) || ticksLived > 12000)
                        //                        if (this.dropItem && this.world.getGameRules().getBoolean("doEntityDrops"))
//                            this.a((IMaterial) block);
                        die();
                }
                else
                {
                    IBlockData iblockdata = world.getType(blockposition);

                    // PIM: Changed because I don't trust moving it downwards.
//                    if (!flag1 && BlockFalling.k(this.world.getType(new BlockPosition(this.locX, this.locY - 0.009999999776482582D, this.locZ))))
//                    {
//                        this.onGround = false;
//                        return;
//                    }

                    Vec3D newMot = getMot();
//                    newMot.x *= 0.699999988079071D;
//                    newMot.z *= 0.699999988079071D;
////                    this.motY *= 0.699999988079071D;
//                    newMot.y *= 0.0D;
                    double motX = newMot.x * 0.699999988079071D;
                    double motY = newMot.y * 0.0D;
                    double motZ = newMot.z * 0.699999988079071D;
                    this.setMot(motX, motY, motZ);

//                    this.motY *= -0.5D; // PIM: Changed because I'm all for equality and such.
                    // Errr, this is not a moving piston by definition, right?
                    if (iblockdata.getBlock() != Blocks.MOVING_PISTON)
                    {
                        die();
                        if (block instanceof BlockFalling)
                            ((BlockFalling) block).a(world, blockposition);
                    }
                }
            }

            Vec3D mot = getMot();
            double motX = mot.x * 0.9800000190734863D;
            double motY = mot.y * 1.0D;
            double motZ = mot.z * 0.9800000190734863D;

//            mot.x *= 0.9800000190734863D;
////            this.motY *= 0.9800000190734863D;
//            mot.y *= 1.0D;
////            this.motY  = 1.0D;
//            mot.z *= 0.9800000190734863D;
            setMot(motX, motY, motZ);
        }
    }

    @Override
    protected void b(NBTTagCompound nbttagcompound)
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
    protected void a(NBTTagCompound nbttagcompound)
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
    public void a(boolean flag)
    {
        hurtEntities = flag;
    }

    @Override
    public void appendEntityCrashDetails(CrashReportSystemDetails crashreportsystemdetails)
    {
        super.appendEntityCrashDetails(crashreportsystemdetails);
        crashreportsystemdetails.a("Immitating BlockState", block.toString());
    }

    @Override
    public IBlockData getBlock()
    {
        return block;
    }
}
