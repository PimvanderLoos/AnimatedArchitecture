package nl.pim16aap2.bigDoors.NMS.v1_13_R2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.Lists;

import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.BlockAnvil;
import net.minecraft.server.v1_13_R2.BlockConcretePowder;
import net.minecraft.server.v1_13_R2.BlockFalling;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Blocks;
import net.minecraft.server.v1_13_R2.CrashReportSystemDetails;
import net.minecraft.server.v1_13_R2.DamageSource;
import net.minecraft.server.v1_13_R2.DataWatcher;
import net.minecraft.server.v1_13_R2.DataWatcherObject;
import net.minecraft.server.v1_13_R2.DataWatcherRegistry;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityFallingBlock;
import net.minecraft.server.v1_13_R2.EnumDirection;
import net.minecraft.server.v1_13_R2.EnumMoveType;
import net.minecraft.server.v1_13_R2.FluidCollisionOption;
import net.minecraft.server.v1_13_R2.GameProfileSerializer;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.ITileEntity;
import net.minecraft.server.v1_13_R2.MathHelper;
import net.minecraft.server.v1_13_R2.MovingObjectPosition;
import net.minecraft.server.v1_13_R2.NBTBase;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.TagsBlock;
import net.minecraft.server.v1_13_R2.TagsFluid;
import net.minecraft.server.v1_13_R2.TileEntity;
import net.minecraft.server.v1_13_R2.Vec3D;
import nl.pim16aap2.bigDoors.BigDoors;
import nl.pim16aap2.bigDoors.NMS.CustomEntityFallingBlock_Vall;

/*
 * This is a custom falling block entity.
 * Changes:
 * - It does not die after a specified (600) amount of ticks.
 * - Easy kill() function to kill it.
 * - Not affected by gravity.
 * - Does not hurt entities (by default, anyway).
 * - NoClip enabled
 */

public class CustomEntityFallingBlock_V1_13_R2 extends net.minecraft.server.v1_13_R2.EntityFallingBlock implements CustomEntityFallingBlock_Vall
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
    private final BigDoors plugin;

    public CustomEntityFallingBlock_V1_13_R2(BigDoors plugin, org.bukkit.World world, double d0, double d1, double d2, IBlockData iblockdata)
    {
        super(((CraftWorld) world).getHandle());
        this.plugin = plugin;
        bukkitWorld = world;
        block = iblockdata;
        j = true;
        setSize(0.98F, 0.98F);
        setPosition(d0, d1 + (1.0F - length) / 2.0F, d2);
        dropItem = false;
        noclip = true;
        setNoGravity(true);
        fallHurtMax = 0;
        fallHurtAmount = 0.0F;
        motX = 0.0D;
        motY = 0.0D;
        motZ = 0.0D;
        lastX = d0;
        lastY = d1;
        lastZ = d2;
        this.a(new BlockPosition(this));
        spawn();
    }

    @Override
    public void die()
    {
//        // This part doesn't really work as well as I'd hoped.
//        // The added velocity needs to depend on block speed.
//        AxisAlignedBB Oldbb = this.getBoundingBox();
//        AxisAlignedBB bb = new AxisAlignedBB(Oldbb.a, Oldbb.b, Oldbb.c, Oldbb.d, Oldbb.e + 0.2, Oldbb.f);
//
//        List<Entity> list = this.world.getEntities(this, bb);
//        if (!list.isEmpty())
//        {
//            Iterator<Entity> iterator = list.iterator();
//            while (iterator.hasNext())
//            {
//                Entity entity = (Entity) iterator.next();
//                if (entity instanceof EntityPlayer)
//                {
////                    entity.getBukkitEntity().teleport(new Location(bukkitWorld, entity.locX, (int) (bb.e + 0.1) + 1, entity.locZ));
////                    Bukkit.broadcastMessage("FBMoty = " + this.motY + ", playerMotY = " +
////                            entity.motY + ", new motY = " + (entity.motY + Math.abs(this.motY) * 1.1));
//                    entity.motY += Math.abs(this.motY) * 1.1;
//                    entity.velocityChanged = true;
//                }
//            }
//        }

        for (Entity ent : passengers)
        {
            ent.dead = true;
        }

        dead = true;
    }

    public void spawn()
    {
        ((org.bukkit.craftbukkit.v1_13_R2.CraftWorld) bukkitWorld).getHandle().addEntity(this, SpawnReason.CUSTOM);
    }

    @Override
    public boolean bk()
    {
        return false;
    }

    @Override
    public void a(BlockPosition blockposition)
    {
        datawatcher.set(EntityFallingBlock.d, blockposition);
    }

    @Override
    protected boolean playStepSound()
    {
        return false;
    }

    @Override
    protected void x_()
    {
        datawatcher.register(EntityFallingBlock.d, BlockPosition.ZERO);
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
        double minX = bb.a;
        double minY = bb.b;
        double minZ = bb.c;
        double maxX = bb.d;
        double maxY = bb.e;
        double maxZ = bb.f;
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
                if (entity instanceof CustomEntityFallingBlock_V1_13_R2)
                    ret.add(entity);
            }
        }
        return ret;
    }

    @SuppressWarnings("unused")
    private void disableGravity(CraftPlayer player)
    {
        player.setGravity(false);
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                player.setGravity(true);
            }
        }.runTaskLater(plugin, 20);
    }

    @SuppressWarnings("rawtypes")
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
                    world.setAir(blockposition);
            }

            // If gravity (not no gravity), DO NOT apply negative y speed.
            if (!isNoGravity())
                motY -= 0.03999999910593033D;
//                this.motY -= 0.0D;

            move(EnumMoveType.SELF, motX, motY, motZ);
            if (!world.isClientSide)
            {
                blockposition = new BlockPosition(this);
                boolean isConcretePowder = this.block.getBlock() instanceof BlockConcretePowder;
                // TODO: Look into this. Is this really needed? Might it interfere??
                boolean flag1 = isConcretePowder && world.b(blockposition).a(TagsFluid.WATER); // Concrete in powder?
                double d0 = motX * motX + motY * motY + motZ * motZ;

                if (isConcretePowder && d0 > 1.0D)
                {
                    MovingObjectPosition movingobjectposition = world.rayTrace(new Vec3D(lastX, lastY, lastZ), new Vec3D(locX, locY, locZ), FluidCollisionOption.SOURCE_ONLY);
                    if (movingobjectposition != null && world.b(movingobjectposition.a()).a(TagsFluid.WATER))
                    {
                        blockposition = movingobjectposition.a();
                        flag1 = true;
                    }
                }

                if (!onGround && !flag1)
                {
//                    if (this.ticksLived > 100 && !this.world.isClientSide && (blockposition.getY() < 1 || blockposition.getY() > 256) || this.ticksLived > 600)
                    // PIM: Changed to make them live longer (12k ticks instead of 600 -> 10min instead of .5min).
                    if (ticksLived > 100 && !world.isClientSide && (blockposition.getY() < 1 || blockposition.getY() > 256) || ticksLived > 12000)
                    {
//                        if (this.dropItem && this.world.getGameRules().getBoolean("doEntityDrops"))
//                            this.a((IMaterial) block);
                        die();
                    }
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

                    motX *= 0.699999988079071D;
                    motZ *= 0.699999988079071D;
//                    this.motY *= 0.699999988079071D;
                    motY *= 0.0D;
//                    this.motY *= -0.5D; // PIM: Changed because I'm all for equality and such.
                    // Errr, this is not a moving piston by definition, right?
                    if (iblockdata.getBlock() != Blocks.MOVING_PISTON)
                    {
                        die();
                        if (!f)
                        {
                            if (iblockdata.getMaterial().isReplaceable() && (flag1 || !BlockFalling.k(world.getType(blockposition.down()))) && world.setTypeAndData(blockposition, this.block, 3))
                            {
                                if (block instanceof BlockFalling)
                                    ((BlockFalling) block).a(world, blockposition, this.block, iblockdata);

                                if (tileEntityData != null && block instanceof ITileEntity)
                                {
                                    TileEntity tileentity = world.getTileEntity(blockposition);
                                    if (tileentity != null)
                                    {
                                        NBTTagCompound nbttagcompound = tileentity.save(new NBTTagCompound());
                                        Iterator iterator = tileEntityData.getKeys().iterator();

                                        while (iterator.hasNext())
                                        {
                                            String s = (String) iterator.next();
                                            NBTBase nbtbase = tileEntityData.get(s);

                                            if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s))
                                                nbttagcompound.set(s, nbtbase.clone());
                                        }

                                        tileentity.load(nbttagcompound);
                                        tileentity.update();
                                    }
                                }
                            }
                            else if (dropItem && world.getGameRules().getBoolean("doEntityDrops"))
                                this.a(block);
                        }
                        else if (block instanceof BlockFalling)
                        {
                            ((BlockFalling) block).a(world, blockposition);
                        }
                    }
                }
            }

            motX *= 0.9800000190734863D;
//            this.motY *= 0.9800000190734863D;
            motY *= 1.0D;
//            this.motY  = 1.0D;
            motZ *= 0.9800000190734863D;
        }

//        {
//            AxisAlignedBB Oldbb = this.getBoundingBox();
//            AxisAlignedBB bb = new AxisAlignedBB(Oldbb.a, Oldbb.b, Oldbb.c, Oldbb.d, Oldbb.e + 0.2, Oldbb.f);
//
//            List list = this.world.getEntities(this, bb);
//            if (!list.isEmpty())
//            {
//                Iterator iterator = list.iterator();
//                while (iterator.hasNext())
//                {
//                    Entity entity = (Entity) iterator.next();
//
//                    if (entity instanceof EntityPlayer)
//                    {
//                        EntityPlayer player = (EntityPlayer) entity;
//
//                        // 0.5D Should not be added here, as the BoundingBox does not include those either, it seems.
//                        double elocX = entity.locX;
//                        double elocY = entity.locY;
//                        double elocZ = entity.locZ;
//
//                        double bbMinX = bb.a;
//                        double bbMinY = bb.b;
//                        double bbMinZ = bb.c;
//                        double bbMaxX = bb.d;
//                        double bbMaxY = bb.e;
//                        double bbMaxZ = bb.f;
//
//                        double d0 = 0, d1 = 0, d2 = 0;
//
//                        boolean inX = elocX < bbMaxX && elocX > bbMinX;
//                        boolean inY = elocX < bbMaxX && elocX > bbMinX;
//                        boolean inZ = elocZ < bbMaxZ && elocZ > bbMinZ;
//
//                        // TODO: Give this thing a sense of unity.
//                        // Check for nearby blocks and their position, so players aren't pushed into other blocks.
//                        // Or maybe use a single (custom) bounding box for the entire door?
//                        // Maybe make the player ride an armorstand and make that move properly?
//                        if (getFallingBlocksOnSide(bb, EnumDirection.UP).size() == 0 || 1 == 1)
//                        {
//                            disableGravity(player.getBukkitEntity());
//
//                            double diff    = bb.e - (elocY - 0.1);
//                            diff = diff > 0 ? diff : 0;
//                            diff /= 10;
//                            double matchFB = (this.motY > 0 ? this.motY : -0.05) * 1.01;
//                            double currVel = entity.motY > 0 ? entity.motY : 0;
//
//                            double yVelocity = (diff + matchFB + currVel);
//
//                            if (Math.abs(yVelocity) > Math.abs(this.motY) * 1.1)
//                                yVelocity = this.motY * 1.1;
//
//                            player.setAirTicks(0);
//                            player.fallDistance = 0;
//                            player.onGround = true;
//                            d1 = yVelocity;
//                        }
//    //                    else
//    //                    {
//    //                        if (inX)
//    //                            d0 = Math.abs(elocX - bbMaxX) > Math.abs(elocX - bbMinX) ?
//    //                                    elocX - bbMaxX : elocX - bbMinX;
//    //                        if (inZ)
//    //                            d2 = Math.abs(elocZ - bbMaxZ) > Math.abs(elocZ - bbMinZ) ?
//    //                                    elocZ - bbMaxZ : elocZ - bbMinZ;
//    //                    }
//                        player.motX += d0/10;
//                        player.motY  = d1;
//                        player.motZ += d2/10;
//                        player.velocityChanged = true;
//                    }
//                }
//            }
//        }


//        {
//        AxisAlignedBB Oldbb = this.getBoundingBox();
//        AxisAlignedBB bb = new AxisAlignedBB(Oldbb.a, Oldbb.b, Oldbb.c, Oldbb.d, Oldbb.e + 0.2, Oldbb.f);
//
//        List list = this.world.getEntities(this, bb);
//        if (!list.isEmpty())
//        {
//            Iterator iterator = list.iterator();
//            while (iterator.hasNext())
//            {
//                Entity entity = (Entity) iterator.next();
//
//                if (entity instanceof EntityPlayer)
//                {
//                    EntityPlayer player = (EntityPlayer) entity;
//                    // 0.5D Should not be added here, as the BoundingBox does not include those either, it seems.
//                    double elocX = entity.locX;
//                    double elocY = entity.locY;
//                    double elocZ = entity.locZ;
//
//                    double bbMinX = bb.a;
//                    double bbMinY = bb.b;
//                    double bbMinZ = bb.c;
//                    double bbMaxX = bb.d;
//                    double bbMaxY = bb.e;
//                    double bbMaxZ = bb.f;
//
//                    double d0 = 0, d1 = 0, d2 = 0;
//
//                    boolean inX = elocX < bbMaxX && elocX > bbMinX;
//                    boolean inY = elocX < bbMaxX && elocX > bbMinX;
//                    boolean inZ = elocZ < bbMaxZ && elocZ > bbMinZ;
//
//                    // TODO: Give this thing a sense of unity.
//                    // Check for nearby blocks and their position, so players aren't pushed into other blocks.
//                    // Or maybe use a single (custom) bounding box for the entire door?
//                    // Maybe make the player ride an armorstand and make that move properly?
//                    if (getFallingBlocksOnSide(bb, EnumDirection.UP).size() == 0)
//                    {
//                        disableGravity(player.getBukkitEntity());
//
//                        double diff    = bb.e - (elocY - 0.1);
//                        diff = diff > 0 ? diff : 0;
//                        diff /= 10;
//                        double matchFB = (this.motY > 0 ? this.motY : -0.05) * 1.01;
//                        double currVel = entity.motY > 0 ? entity.motY : 0;
//
//                        double yVelocity = (diff + matchFB + currVel);
//
//                        if (Math.abs(yVelocity) > Math.abs(this.motY) * 1.1)
//                            yVelocity = this.motY * 1.1;
//
//                        player.setAirTicks(0);
//                        player.fallDistance = 0;
//                        player.onGround = true;
////                        player.motY = yVelocity;
////                        player.velocityChanged = true;
//                        d1 = yVelocity;
//                    }
////                    else
////                    {
////                        if (inX)
////                            d0 = Math.abs(elocX - bbMaxX) > Math.abs(elocX - bbMinX) ?
////                                    elocX - bbMaxX : elocX - bbMinX;
////                        if (inZ)
////                            d2 = Math.abs(elocZ - bbMaxZ) > Math.abs(elocZ - bbMinZ) ?
////                                    elocZ - bbMaxZ : elocZ - bbMinZ;
////                    }
//                    player.motX += d0/10;
//                    player.motY  = d1;
//                    player.motZ += d2/10;
//                    player.velocityChanged = true;
//                }
//            }
//        }
//    }

    }

    @SuppressWarnings("rawtypes")
    @Override
    public void c(float f, float f1)
    {
        if (hurtEntities)
        {
            int i = MathHelper.f(f - 1.0F);

            if (i > 0)
            {
                ArrayList arraylist = Lists.newArrayList(world.getEntities(this, getBoundingBox()));
                boolean flag = block.a(TagsBlock.ANVIL);
                DamageSource damagesource = flag ? DamageSource.ANVIL : DamageSource.FALLING_BLOCK;
                Iterator iterator = arraylist.iterator();

                while (iterator.hasNext())
                {
                    Entity entity = (Entity) iterator.next();
                    entity.damageEntity(damagesource, Math.min(MathHelper.d(i * fallHurtAmount), fallHurtMax));
                }

                if (flag && random.nextFloat() < 0.05000000074505806D + i * 0.05D)
                {
                    IBlockData iblockdata = BlockAnvil.a_(block);

                    if (iblockdata == null)
                        this.f = true;
                    else
                        block = iblockdata;
                }
            }
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

    @Override
    public boolean bM()
    {
        return true;
    }
}
