package nl.pim16aap2.bigDoors.NMS.v1_12_R1;

import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.Blocks;
import net.minecraft.server.v1_12_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EnumMoveType;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.Material;
import net.minecraft.server.v1_12_R1.MathHelper;
import net.minecraft.server.v1_12_R1.MinecraftKey;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import nl.pim16aap2.bigDoors.NMS.CustomEntityFallingBlock_Vall;

import javax.annotation.Nullable;

import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

/*
 * This is a custom falling block entity.
 * Changes:
 * - It does not die after a specified (600) amount of ticks.
 * - Easy kill() function to kill it.
 * - Not affected by gravity.
 * - Does not hurt entities (by default, anyway).
 * - NoClip enabled
 */

public class CustomEntityFallingBlock_V1_12_R1 extends net.minecraft.server.v1_12_R1.EntityFallingBlock implements CustomEntityFallingBlock_Vall
{
	private        IBlockData block      ;
	private int    fallHurtMax    = 40   ;
	private float  fallHurtAmount = 2.0F ;
	public boolean dropItem       = false;
	public boolean hurtEntities   = false;
	public NBTTagCompound tileEntityData ;
	private org.bukkit.World  bukkitWorld;

	public CustomEntityFallingBlock_V1_12_R1(org.bukkit.World world)
	{
		super((net.minecraft.server.v1_12_R1.World) world);
		setNoGravity(true);
		this.noclip = true;
		this.bukkitWorld  = world;
		spawn();
	}
	
	@SuppressWarnings("deprecation")
	public CustomEntityFallingBlock_V1_12_R1(org.bukkit.World world, org.bukkit.Material mat, double d0, double d1, double d2, byte data)
	{
		super(((CraftWorld) world).getHandle(), d0, d1, d2, CraftMagicNumbers.getBlock(mat).fromLegacyData(data));
		this.block  = CraftMagicNumbers.getBlock(mat).fromLegacyData(data);
		this.i      = true;
		this.setSize(0.98F, 0.98F);
		this.setPosition(d0, d1 + (double) ((1.0F - this.length) / 2.0F), d2);
		this.motX   = 0.0D;
		this.motY   = 0.0D;
		this.motZ   = 0.0D;
		this.lastX  = d0;
		this.lastY  = d1;
		this.lastZ  = d2;
		setNoGravity(true);
		this.noclip = true;
		this.bukkitWorld  = world;
		spawn();
	}
	
	public void spawn()
	{
		((org.bukkit.craftbukkit.v1_12_R1.CraftWorld) this.bukkitWorld).getHandle().addEntity(this, SpawnReason.CUSTOM);
	}
	
	public void kill()
	{
		this.die();
	}
	
	@Override
    public boolean inBlock() 
	{
        return false;
    }
	
	@Override
    public void collide(Entity entity) 
	{
        if (!this.x(entity)) 
        {
            if (!entity.noclip && !this.noclip) 
            {
            		double d0 = entity.locX - this.locX;
            		double d1 = entity.locZ - this.locZ;
            		double d2 = MathHelper.a(d0, d1);

                if (d2 >= 0.009999999776482582D) 
                {
                    d2 = (double) MathHelper.sqrt(d2);
                    d0 /= d2;
                    d1 /= d2;
                    double d3 = 1.0D / d2;

                    if (d3 > 1.0D)
                        d3 = 1.0D;

                    d0 *= d3;
                    d1 *= d3;
                    d0 *= 0.05000000074505806D;
                    d1 *= 0.05000000074505806D;
                    d0 *= (double) (1.0F - this.R);
                    d1 *= (double) (1.0F - this.R);
                    if (!this.isVehicle())
                        this.f(-d0, 0.0D, -d1);

                    if (!entity.isVehicle())
                        entity.f(d0, 0.0D, d1);
                }
            }
        }
    }

	public boolean bd()
	{
		return false;
	}

	@Override
	public void a(BlockPosition blockposition)
	{
		this.datawatcher.set(CustomEntityFallingBlock_V1_12_R1.d, blockposition);
	}

	@Override
	protected boolean playStepSound()
	{
		return false;
	}

	@Override
	protected void i()
	{
		this.datawatcher.register(CustomEntityFallingBlock_V1_12_R1.d, BlockPosition.ZERO);
	}

	@Override
	public boolean isInteractable()
	{
		return !this.dead;
	}

	@Override
	public void B_()
	{
		if (this.block.getMaterial() == Material.AIR)
			this.die();
		else
		{
			this.lastX = this.locX;
			this.lastY = this.locY;
			this.lastZ = this.locZ;
			BlockPosition blockposition;

			this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
			this.motX *= 0.9800000190734863D;
			this.motY *= 0.9800000190734863D;
			this.motZ *= 0.9800000190734863D;
			if (!this.world.isClientSide)
			{
				blockposition = new BlockPosition(this);
				if (this.onGround)
				{
					// Do nothing here, it should never be considered to be on the ground anyway.
				}
				else if (this.ticksLived > 100 && !this.world.isClientSide && (blockposition.getY() < 1 || blockposition.getY() > 256))
					this.die();
			}
		}
	}

	@Override
	public void e(float f, float f1)
	{
		// This method takes care of calculating damage, so ignore it.
	}

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
			this.block  = Block.getByName(nbttagcompound.getString("Block")).fromLegacyData(i);
		else if (nbttagcompound.hasKeyOfType("TileID", 99))
			this.block  = Block.getById(nbttagcompound.getInt("TileID")).fromLegacyData(i);
		else
			this.block  = Block.getById(nbttagcompound.getByte("Tile") & 255).fromLegacyData(i);

		this.ticksLived = nbttagcompound.getInt("Time");
		Block block     = this.block.getBlock();

		if (nbttagcompound.hasKeyOfType("HurtEntities", 99))
		{
			this.hurtEntities   = nbttagcompound.getBoolean("HurtEntities");
			this.fallHurtAmount = nbttagcompound.getFloat  ("FallHurtAmount");
			this.fallHurtMax    = nbttagcompound.getInt    ("FallHurtMax");
		}
//		else if (block == Blocks.ANVIL)
//			this.hurtEntities = true;

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

			crashreportsystemdetails.a("Immitating block ID",   (Object) Integer.valueOf(Block.getId(block)));
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
	public boolean bC()
	{
		return true;
	}
}
