package nl.pim16aap2.bigDoors.customEntities;

import com.google.common.collect.Lists;

import net.minecraft.server.v1_11_R1.Block;
import net.minecraft.server.v1_11_R1.BlockAnvil;
import net.minecraft.server.v1_11_R1.BlockFalling;
import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.Blocks;
import net.minecraft.server.v1_11_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_11_R1.DamageSource;
import net.minecraft.server.v1_11_R1.DataConverterManager;
import net.minecraft.server.v1_11_R1.Entity;
import net.minecraft.server.v1_11_R1.EnumDirection;
import net.minecraft.server.v1_11_R1.EnumMoveType;
import net.minecraft.server.v1_11_R1.IBlockData;
import net.minecraft.server.v1_11_R1.ITileEntity;
import net.minecraft.server.v1_11_R1.Material;
import net.minecraft.server.v1_11_R1.MathHelper;
import net.minecraft.server.v1_11_R1.MinecraftKey;
import net.minecraft.server.v1_11_R1.NBTBase;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.TileEntity;

import java.util.ArrayList;
import java.util.Iterator;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_11_R1.util.CraftMagicNumbers;

/*
 * This is a custom falling block entity.
 * Changes:
 * - It does not die after a specified (600) amount of ticks.
 * - Easy kill() function to kill it.
 * - Not affected by gravity.
 * - Does not hurt entities (by default, anyway).
 * 
 */

public class CustomEntityFallingBlock extends net.minecraft.server.v1_11_R1.EntityFallingBlock
{

	private IBlockData block;
	private boolean f;
	private int fallHurtMax = 40;
	private float fallHurtAmount = 2.0F;
	public NBTTagCompound tileEntityData;
	public boolean dropItem = false;
	public boolean hurtEntities = false;

	public CustomEntityFallingBlock(World world)
	{
		super((net.minecraft.server.v1_11_R1.World) world);
	}

	@SuppressWarnings("deprecation")
	public CustomEntityFallingBlock(World world, org.bukkit.Material mat, double d0, double d1, double d2, byte data)
	{
		super(((CraftWorld) world).getHandle(), d0, d1, d2, CraftMagicNumbers.getBlock(mat).fromLegacyData(data));
		this.block = CraftMagicNumbers.getBlock(mat).fromLegacyData(data);
		this.i = true;
		this.setSize(0.98F, 0.98F);
		this.setPosition(d0, d1 + (double) ((1.0F - this.length) / 2.0F), d2);
		this.motX = 0.0D;
		this.motY = 0.0D;
		this.motZ = 0.0D;
		this.lastX = d0;
		this.lastY = d1;
		this.lastZ = d2;
//		Bukkit.broadcastMessage("Material = " + mat.toString() + ", block = " + block.toString());
	}
	
	public void kill()
	{
		System.out.println("0: Kill");
		this.die();
	}
	
	// First attempt at getting accurate location readings
	public Location getLocaction1()
	{
		return new Location(world.getWorld(), lastX, lastY, lastZ);
	}
	
	// Second attempt at getting accurate location readings
	public Location getLocaction2()
	{
		this.recalcPosition();
		return new Location(world.getWorld(), lastX, lastY, lastZ);
	}
	
	// Third attempt at getting accurate location readings
	public Location getLocaction3()
	{
		this.recalcPosition();
		BlockPosition blockposition = new BlockPosition(this);
		return new Location(world.getWorld(), (double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ());
	}
	
	// Fourth attempt at getting accurate location readings
	public Location getLocaction4()
	{
		this.recalcPosition();
		BlockPosition blockposition = new BlockPosition(this);
		return new Location(world.getWorld(), (double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ());
	}
	
	
	


	public boolean bd()
	{
		return false;
	}

	@Override
	public void a(BlockPosition blockposition)
	{
		this.datawatcher.set(CustomEntityFallingBlock.d, blockposition);
	}

	@Override
	protected boolean playStepSound()
	{
		return false;
	}

	@Override
	protected void i()
	{
		this.datawatcher.register(CustomEntityFallingBlock.d, BlockPosition.ZERO);
	}

	@Override
	public boolean isInteractable()
	{
		return !this.dead;
	}

	@Override
	public void A_()
	{
		Block block = this.block.getBlock();

		if (this.block.getMaterial() == Material.AIR)
			this.die();
		else
		{
			this.lastX = this.locX;
			this.lastY = this.locY;
			this.lastZ = this.locZ;
			BlockPosition blockposition;

//			if (this.ticksLived == 0)
//			{
//				blockposition = new BlockPosition(this);
//				if (this.world.getType(blockposition).getBlock() == block)
//					this.world.setAir(blockposition);
//				else if (!this.world.isClientSide)
//				{
//					System.out.println("1: Kill");
//					this.die();
//					return;
//				}
//			}
			
//			// Disabling gravity.
//			if (!this.isNoGravity())
//			{
//                this.motY -= 0.03999999910593033D;
//			}

			this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
			this.motX *= 0.9800000190734863D;
			this.motY *= 0.9800000190734863D;
			this.motZ *= 0.9800000190734863D;
			if (!this.world.isClientSide)
			{
				blockposition = new BlockPosition(this);
				if (this.onGround)
				{
					IBlockData iblockdata = this.world.getType(blockposition);

//					if (BlockFalling.i(this.world.getType(new BlockPosition(this.locX, this.locY - 0.009999999776482582D, this.locZ))))
					// Disabling gravity.
					if (BlockFalling.i(this.world.getType(new BlockPosition(this.locX, this.locY, this.locZ))))
					{
						this.onGround = false;
						return;
					}

					this.motX *= 0.699999988079071D;
					this.motZ *= 0.699999988079071D;
//					this.motY *= -0.5D;
					// Not sure what this does, but now it's the same as the other two directions.
					this.motY *= 0.699999988079071D;
					if (iblockdata.getBlock() != Blocks.PISTON_EXTENSION)
					{
						System.out.println("2: Kill");
						this.die();
						if (!this.f)
						{
							if (this.world.a(block, blockposition, true, EnumDirection.UP, (Entity) null) && !BlockFalling.i(this.world.getType(blockposition.down())) && this.world.setTypeAndData(blockposition, this.block, 3))
							{
								if (block instanceof BlockFalling)
									((BlockFalling) block).a_(this.world, blockposition);

								if (this.tileEntityData != null && block instanceof ITileEntity)
								{
									TileEntity tileentity = this.world.getTileEntity(blockposition);

									if (tileentity != null)
									{
										NBTTagCompound nbttagcompound = tileentity.save(new NBTTagCompound());
										@SuppressWarnings("rawtypes")
										Iterator iterator = this.tileEntityData.c().iterator();

										while (iterator.hasNext())
										{
											String s = (String) iterator.next();
											NBTBase nbtbase = this.tileEntityData.get(s);

											if (!"x".equals(s) && !"y".equals(s) && !"z".equals(s))
												nbttagcompound.set(s, nbtbase.clone());
										}

										tileentity.a(nbttagcompound);
										tileentity.update();
									}
								}
							}
						}
						else if (block instanceof BlockFalling)
							((BlockFalling) block).b(this.world, blockposition);
					}
				}
				else if (this.ticksLived > 100 && !this.world.isClientSide && (blockposition.getY() < 1 || blockposition.getY() > 256))
				{
					System.out.println("3: Kill");
					this.die();
				}
			}
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void e(float f, float f1)
	{
		Block block = this.block.getBlock();

		// Disabled by default: 
		if (this.hurtEntities)
		{
			int i = MathHelper.f(f - 1.0F);

			if (i > 0)
			{
				ArrayList arraylist = Lists.newArrayList(this.world.getEntities(this, this.getBoundingBox()));
				boolean flag = block == Blocks.ANVIL;
				DamageSource damagesource = flag ? DamageSource.ANVIL : DamageSource.FALLING_BLOCK;
				Iterator iterator = arraylist.iterator();

				while (iterator.hasNext())
				{
					Entity entity = (Entity) iterator.next();
					entity.damageEntity(damagesource, (float) Math.min(MathHelper.d((float) i * this.fallHurtAmount), this.fallHurtMax));
				}

				if (flag && (double) this.random.nextFloat() < 0.05000000074505806D + (double) i * 0.05D)
				{
					int j = ((Integer) this.block.get(BlockAnvil.DAMAGE)).intValue();
					++j;
					if (j > 2)
						this.f = true;
					else
						this.block = this.block.set(BlockAnvil.DAMAGE, Integer.valueOf(j));
				}
			}
		}
	}

	public static void a(DataConverterManager dataconvertermanager)
	{}

	@Override
	protected void b(NBTTagCompound nbttagcompound)
	{
		Block block = this.block != null ? this.block.getBlock() : Blocks.AIR;
		MinecraftKey minecraftkey = (MinecraftKey) Block.REGISTRY.b(block);

		nbttagcompound.setString ("Block",          minecraftkey == null ? "" : minecraftkey.toString());
		nbttagcompound.setByte   ("Data",           (byte) block.toLegacyData(this.block));
		nbttagcompound.setInt    ("Time",           this.ticksLived);
		nbttagcompound.setBoolean("DropItem",       this.dropItem);
		nbttagcompound.setBoolean("HurtEntities",   this.hurtEntities);
		nbttagcompound.setFloat  ("FallHurtAmount", this.fallHurtAmount);
		nbttagcompound.setInt    ("FallHurtMax",    this.fallHurtMax);
		if (this.tileEntityData != null)
			nbttagcompound.set("TileEntityData", this.tileEntityData);

	}

	@SuppressWarnings("deprecation")
	@Override
	protected void a(NBTTagCompound nbttagcompound)
	{
		int i = nbttagcompound.getByte("Data") & 255;

		if (nbttagcompound.hasKeyOfType("Block", 8))
			this.block = Block.getByName(nbttagcompound.getString("Block")).fromLegacyData(i);
		else if (nbttagcompound.hasKeyOfType("TileID", 99))
			this.block = Block.getById(nbttagcompound.getInt("TileID")).fromLegacyData(i);
		else
			this.block = Block.getById(nbttagcompound.getByte("Tile") & 255).fromLegacyData(i);

		this.ticksLived = nbttagcompound.getInt("Time");
		Block block = this.block.getBlock();

		if (nbttagcompound.hasKeyOfType("HurtEntities", 99))
		{
			this.hurtEntities = nbttagcompound.getBoolean("HurtEntities");
			this.fallHurtAmount = nbttagcompound.getFloat("FallHurtAmount");
			this.fallHurtMax = nbttagcompound.getInt("FallHurtMax");
		}
		else if (block == Blocks.ANVIL)
			this.hurtEntities = true;

		if (nbttagcompound.hasKeyOfType("DropItem", 99))
			this.dropItem = nbttagcompound.getBoolean("DropItem");

		if (nbttagcompound.hasKeyOfType("TileEntityData", 10))
			this.tileEntityData = nbttagcompound.getCompound("TileEntityData");

		if (block == null || block.getBlockData().getMaterial() == Material.AIR)
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
		if (this.block != null)
		{
			Block block = this.block.getBlock();

			crashreportsystemdetails.a("Immitating block ID", (Object) Integer.valueOf(Block.getId(block)));
			crashreportsystemdetails.a("Immitating block data", (Object) Integer.valueOf(block.toLegacyData(this.block)));
		}
	}

	@Override
	@Nullable
	public IBlockData getBlock()
	{
		return this.block;
	}

	@Override
	public boolean bu()
	{
		return true;
	}
}
