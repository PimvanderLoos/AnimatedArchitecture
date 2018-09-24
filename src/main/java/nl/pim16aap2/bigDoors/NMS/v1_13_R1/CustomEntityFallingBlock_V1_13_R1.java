package nl.pim16aap2.bigDoors.NMS.v1_13_R1;

import java.util.ArrayList;
import java.util.Iterator;

import org.bukkit.craftbukkit.v1_13_R1.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.google.common.collect.Lists;

import net.minecraft.server.v1_13_R1.Block;
import net.minecraft.server.v1_13_R1.BlockAnvil;
import net.minecraft.server.v1_13_R1.BlockConcretePowder;
import net.minecraft.server.v1_13_R1.BlockFalling;
import net.minecraft.server.v1_13_R1.BlockPosition;
import net.minecraft.server.v1_13_R1.Blocks;
import net.minecraft.server.v1_13_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_13_R1.DamageSource;
import net.minecraft.server.v1_13_R1.DataWatcher;
import net.minecraft.server.v1_13_R1.DataWatcherObject;
import net.minecraft.server.v1_13_R1.DataWatcherRegistry;
import net.minecraft.server.v1_13_R1.Entity;
import net.minecraft.server.v1_13_R1.EntityFallingBlock;
import net.minecraft.server.v1_13_R1.EnumMoveType;
import net.minecraft.server.v1_13_R1.FluidCollisionOption;
import net.minecraft.server.v1_13_R1.GameProfileSerializer;
import net.minecraft.server.v1_13_R1.IBlockData;
import net.minecraft.server.v1_13_R1.IMaterial;
import net.minecraft.server.v1_13_R1.ITileEntity;
import net.minecraft.server.v1_13_R1.MathHelper;
import net.minecraft.server.v1_13_R1.MovingObjectPosition;
import net.minecraft.server.v1_13_R1.NBTBase;
import net.minecraft.server.v1_13_R1.NBTTagCompound;
import net.minecraft.server.v1_13_R1.TagsBlock;
import net.minecraft.server.v1_13_R1.TagsFluid;
import net.minecraft.server.v1_13_R1.TileEntity;
import net.minecraft.server.v1_13_R1.Vec3D;
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

public class CustomEntityFallingBlock_V1_13_R1 extends net.minecraft.server.v1_13_R1.EntityFallingBlock implements CustomEntityFallingBlock_Vall
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

	//    public CustomEntityFallingBlock_V1_13_R1(net.minecraft.server.v1_13_R1.World world) {
	//        super(EntityTypes.FALLING_BLOCK, world);
	//        this.block = Blocks.SAND.getBlockData();
	//        this.dropItem = true;
	//        this.fallHurtMax = 40;
	//        this.fallHurtAmount = 2.0F;
	//    }

	public CustomEntityFallingBlock_V1_13_R1(org.bukkit.World world, double d0, double d1, double d2, IBlockData iblockdata)
	{
		super(((CraftWorld) world).getHandle());
		this.bukkitWorld = world;
		this.block = iblockdata;
		this.j = true;
		this.setSize(0.98F, 0.98F);
		this.setPosition(d0, d1 + (double) ((1.0F - this.length) / 2.0F), d2);
		this.dropItem = false;
		this.noclip = true;
		this.setNoGravity(true);
		this.fallHurtMax = 0;
		this.fallHurtAmount = 0.0F;
		this.motX = 0.0D;
		this.motY = 0.0D;
		this.motZ = 0.0D;
		this.lastX = d0;
		this.lastY = d1;
		this.lastZ = d2;
		this.a(new BlockPosition(this));
		spawn();
	}

	public void spawn()
	{
		((org.bukkit.craftbukkit.v1_13_R1.CraftWorld) this.bukkitWorld).getHandle().addEntity(this, SpawnReason.CUSTOM);
	}

	@Override
	public boolean bk()
	{
		return false;
	}

	@Override
	public void a(BlockPosition blockposition)
	{
		this.datawatcher.set(EntityFallingBlock.d, blockposition);
	}

	@Override
	protected boolean playStepSound()
	{
		return false;
	}

	@Override
	protected void x_()
	{
		this.datawatcher.register(EntityFallingBlock.d, BlockPosition.ZERO);
	}

	@Override
	public boolean isInteractable()
	{
		return !this.dead;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void tick()
	{
		if (this.block.isAir())
			this.die();
		else
		{
			this.lastX = this.locX;
			this.lastY = this.locY;
			this.lastZ = this.locZ;
			Block block = this.block.getBlock();
			BlockPosition blockposition;

			if (this.ticksLived++ == 0)
			{
				blockposition = new BlockPosition(this);
				if (this.world.getType(blockposition).getBlock() == block)
					this.world.setAir(blockposition);
			}

			// If gravity (not no gravity), DO NOT apply negative y speed.
			if (!this.isNoGravity())
				this.motY -= 0.03999999910593033D;
//				this.motY -= 0.0D;
			
			this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
			if (!this.world.isClientSide)
			{
				blockposition = new BlockPosition(this);
				boolean flag = this.block.getBlock() instanceof BlockConcretePowder;
				boolean flag1 = flag && this.world.b(blockposition).a(TagsFluid.a);
				double d0 = this.motX * this.motX + this.motY * this.motY + this.motZ * this.motZ;

				if (flag && d0 > 1.0D)
				{
					MovingObjectPosition movingobjectposition = this.world.rayTrace(new Vec3D(this.lastX, this.lastY, this.lastZ), new Vec3D(this.locX, this.locY, this.locZ), FluidCollisionOption.SOURCE_ONLY);
					if (movingobjectposition != null && this.world.b(movingobjectposition.a()).a(TagsFluid.a))
					{
						blockposition = movingobjectposition.a();
						flag1 = true;
					}
				}

				if (!this.onGround && !flag1)
				{
//					if (this.ticksLived > 100 && !this.world.isClientSide && (blockposition.getY() < 1 || blockposition.getY() > 256) || this.ticksLived > 600)
					// PIM: Changed to make them live longer (12k ticks instead of 600 -> 10min instead of .5min).
					if (this.ticksLived > 100 && !this.world.isClientSide && (blockposition.getY() < 1 || blockposition.getY() > 256) || this.ticksLived > 12000)
					{
//						if (this.dropItem && this.world.getGameRules().getBoolean("doEntityDrops"))
//							this.a((IMaterial) block);
						this.die();
					}
				}
				else
				{
					IBlockData iblockdata = this.world.getType(blockposition);
					
					// PIM: Changed because I don't trust moving it downwards.
//					if (!flag1 && BlockFalling.k(this.world.getType(new BlockPosition(this.locX, this.locY - 0.009999999776482582D, this.locZ))))
//					{
//						this.onGround = false;
//						return;
//					}

					this.motX *= 0.699999988079071D;
					this.motZ *= 0.699999988079071D;
//					this.motY *= 0.699999988079071D;
					this.motY *= 0.0D;
//					this.motY *= -0.5D; // PIM: Changed because I'm all for equality and such.
					// Errr, this is not a moving piston by definition, right?
					if (iblockdata.getBlock() != Blocks.MOVING_PISTON)
					{
						this.die();
						if (!this.f)
						{
							if (iblockdata.getMaterial().isReplaceable() && (flag1 || !BlockFalling.k(this.world.getType(blockposition.down()))) && this.world.setTypeAndData(blockposition, this.block, 3))
							{
								if (block instanceof BlockFalling)
									((BlockFalling) block).a(this.world, blockposition, this.block, iblockdata);

								if (this.tileEntityData != null && block instanceof ITileEntity)
								{
									TileEntity tileentity = this.world.getTileEntity(blockposition);
									if (tileentity != null)
									{
										NBTTagCompound nbttagcompound = tileentity.save(new NBTTagCompound());
										Iterator iterator = this.tileEntityData.getKeys().iterator();

										while (iterator.hasNext())
										{
											String s = (String) iterator.next();
											NBTBase nbtbase = this.tileEntityData.get(s);

											if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s))
												nbttagcompound.set(s, nbtbase.clone());
										}

										tileentity.load(nbttagcompound);
										tileentity.update();
									}
								}
							}
							else if (this.dropItem && this.world.getGameRules().getBoolean("doEntityDrops"))
								this.a((IMaterial) block);
						}
						else if (block instanceof BlockFalling)
						{
							((BlockFalling) block).a(this.world, blockposition);
						}
					}
				}
			}

			this.motX *= 0.9800000190734863D;
//			this.motY *= 0.9800000190734863D;
			this.motY *= 1.0D;
//			this.motY  = 1.0D;
			this.motZ *= 0.9800000190734863D;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void c(float f, float f1)
	{
		if (this.hurtEntities)
		{
			int i = MathHelper.f(f - 1.0F);

			if (i > 0)
			{
				ArrayList arraylist = Lists.newArrayList(this.world.getEntities(this, this.getBoundingBox()));
				boolean flag = this.block.a(TagsBlock.y);
				DamageSource damagesource = flag ? DamageSource.ANVIL : DamageSource.FALLING_BLOCK;
				Iterator iterator = arraylist.iterator();

				while (iterator.hasNext())
				{
					Entity entity = (Entity) iterator.next();
					entity.damageEntity(damagesource, (float) Math.min(MathHelper.d((float) i * this.fallHurtAmount), this.fallHurtMax));
				}

				if (flag && (double) this.random.nextFloat() < 0.05000000074505806D + (double) i * 0.05D)
				{
					IBlockData iblockdata = BlockAnvil.a_(this.block);

					if (iblockdata == null)
						this.f = true;
					else
						this.block = iblockdata;
				}
			}
		}

	}

	@Override
	protected void b(NBTTagCompound nbttagcompound)
	{
		nbttagcompound.set("BlockState", GameProfileSerializer.a(this.block));
		nbttagcompound.setInt("Time", this.ticksLived);
		nbttagcompound.setBoolean("DropItem", this.dropItem);
		nbttagcompound.setBoolean("HurtEntities", this.hurtEntities);
		nbttagcompound.setFloat("FallHurtAmount", this.fallHurtAmount);
		nbttagcompound.setInt("FallHurtMax", this.fallHurtMax);
		if (this.tileEntityData != null)
			nbttagcompound.set("TileEntityData", this.tileEntityData);

	}

	@Override
	protected void a(NBTTagCompound nbttagcompound)
	{
		this.block = GameProfileSerializer.d(nbttagcompound.getCompound("BlockState"));
		this.ticksLived = nbttagcompound.getInt("Time");
		if (nbttagcompound.hasKeyOfType("HurtEntities", 99))
		{
			this.hurtEntities = nbttagcompound.getBoolean("HurtEntities");
			this.fallHurtAmount = nbttagcompound.getFloat("FallHurtAmount");
			this.fallHurtMax = nbttagcompound.getInt("FallHurtMax");
		}
		else if (this.block.a(TagsBlock.y))
			this.hurtEntities = true;

		if (nbttagcompound.hasKeyOfType("DropItem", 99))
			this.dropItem = nbttagcompound.getBoolean("DropItem");

		if (nbttagcompound.hasKeyOfType("TileEntityData", 10))
			this.tileEntityData = nbttagcompound.getCompound("TileEntityData");

		if (this.block.isAir())
			this.block = Blocks.SAND.getBlockData();

	}

	@Override
	public void a(boolean flag)
	{
		this.hurtEntities = flag;
	}

	@Override
	public void appendEntityCrashDetails(CrashReportSystemDetails crashreportsystemdetails)
	{
		super.appendEntityCrashDetails(crashreportsystemdetails);
		crashreportsystemdetails.a("Immitating BlockState", (Object) this.block.toString());
	}

	@Override
	public IBlockData getBlock()
	{
		return this.block;
	}

	@Override
	public boolean bM()
	{
		return true;
	}
}
