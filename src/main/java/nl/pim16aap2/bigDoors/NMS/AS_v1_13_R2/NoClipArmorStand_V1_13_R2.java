package nl.pim16aap2.bigDoors.NMS.AS_v1_13_R2;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftEntity;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.DamageSource;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityBird;
import net.minecraft.server.v1_13_R2.EnumMoveType;
import net.minecraft.server.v1_13_R2.MathHelper;
import net.minecraft.server.v1_13_R2.MobEffects;
import net.minecraft.server.v1_13_R2.Vec3D;
import nl.pim16aap2.bigDoors.NMS.CustomEntityFallingBlock_Vall;

public class NoClipArmorStand_V1_13_R2 extends net.minecraft.server.v1_13_R2.EntityArmorStand implements CustomEntityFallingBlock_Vall
{
	public boolean isNoGravity;

	public NoClipArmorStand_V1_13_R2(World world, Location loc)
	{
		super(((CraftWorld) world).getHandle());
		this.setPosition(loc.getX(), loc.getY(), loc.getZ());
		this.noclip = true;
		this.isNoGravity = false;
	}
	
	public org.bukkit.entity.Entity getPassenger()
	{
		return isEmpty() ? null : getHandle().passengers.get(0).getBukkitEntity();
	}

	public boolean setPassenger(org.bukkit.entity.Entity passenger)
	{
		com.google.common.base.Preconditions.checkArgument(!this.equals(passenger), "Entity cannot ride itself.");
		if (passenger instanceof CraftEntity)
		{
			eject();
			return ((CraftEntity) passenger).getHandle().startRiding(getHandle());
		}
		else
			return false;
	}

	public boolean eject()
	{
		if (isEmpty())
			return false;

		getHandle().ejectPassengers(); // PAIL: rename
		return true;
	}

	public boolean isEmpty()
	{
		return !getHandle().isVehicle();
	}

	public Entity getHandle()
	{
		return this;
	}

	public List<org.bukkit.entity.Entity> getPassengers()
	{
		return Lists.newArrayList(Lists.transform(getHandle().passengers, new Function<Entity, org.bukkit.entity.Entity>()
		{
			@Override
			public org.bukkit.entity.Entity apply(Entity input)
			{
				return input.getBukkitEntity();
			}
		}));
	}

	public boolean addPassenger(org.bukkit.entity.Entity passenger)
	{
		com.google.common.base.Preconditions.checkArgument(passenger != null, "passenger == null");

		return ((CraftEntity) passenger).getHandle().a(getHandle(), true);
	}

	public boolean removePassenger(org.bukkit.entity.Entity passenger)
	{
		com.google.common.base.Preconditions.checkArgument(passenger != null, "passenger == null");

		((CraftEntity) passenger).getHandle().stopRiding();
		return true;
	}

	@Override
	public void f(double d0, double d1, double d2)
	{
		this.motX += d0;
		this.motY += d1;
		this.motZ += d2;
		this.impulse = true;
	}

//	public void a(float f, float f1, float f2)
//	{
////		if (!this.isNoGravity())
////		{
//			super.a(f, f1, f2);
////		}
//	}
	
	// Overridden from EntityLiving.
	@SuppressWarnings("unused")
	@Override
	public void a(float f, float f1, float f2)
	{
		double d0;
		double d1;
		double d2;

		if (this.cP() || this.bT())
		{
			float f3;
			float f4;
			float f5;

//			if (this.isInWater() && (!(this instanceof EntityHuman) || !((EntityHuman) this).abilities.isFlying))
//			{
//				d2 = this.locY;
//				f4 = this.cx();
//				f3 = 0.02F;
//				f5 = (float) EnchantmentManager.e(this);
//				if (f5 > 3.0F)
//					f5 = 3.0F;
//
//				if (!this.onGround)
//					f5 *= 0.5F;
//
//				if (f5 > 0.0F)
//				{
//					f4 += (0.54600006F - f4) * f5 / 3.0F;
//					f3 += (this.cy() - f3) * f5 / 3.0F;
//				}
//
//				this.b(f, f1, f2, f3);
//				this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
//				this.motX *= (double) f4;
//				this.motY *= 0.800000011920929D;
//				this.motZ *= (double) f4;
//				if (!this.isNoGravity())
//					this.motY -= 0.02D;
//
//				if (this.positionChanged && this.c(this.motX, this.motY + 0.6000000238418579D - this.locY + d2, this.motZ))
//					this.motY = 0.30000001192092896D;
//			}
//			else if (this.au() && (!(this instanceof EntityHuman) || !((EntityHuman) this).abilities.isFlying))
//			{
//				d2 = this.locY;
//				this.b(f, f1, f2, 0.02F);
//				this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
//				this.motX *= 0.5D;
//				this.motY *= 0.5D;
//				this.motZ *= 0.5D;
//				if (!this.isNoGravity())
//					this.motY -= 0.02D;
//
//				if (this.positionChanged && this.c(this.motX, this.motY + 0.6000000238418579D - this.locY + d2, this.motZ))
//					this.motY = 0.30000001192092896D;
//			}
//			else 
			if (this.dc())
			{
				if (this.motY > -0.5D)
					this.fallDistance = 1.0F;

				Vec3D vec3d = this.aN();
				float f6 = this.pitch * 0.017453292F;

				d0 = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
				d1 = Math.sqrt(this.motX * this.motX + this.motZ * this.motZ);
				double d3 = vec3d.b();
				float f7 = MathHelper.cos(f6);

				f7 = (float) ((double) f7 * (double) f7 * Math.min(1.0D, d3 / 0.4D));
				this.motY += -0.08D + (double) f7 * 0.06D;
				double d4;

				if (this.motY < 0.0D && d0 > 0.0D)
				{
					d4 = this.motY * -0.1D * (double) f7;
					this.motY += d4;
					this.motX += vec3d.x * d4 / d0;
					this.motZ += vec3d.z * d4 / d0;
				}

				if (f6 < 0.0F)
				{
					d4 = d1 * (double) (-MathHelper.sin(f6)) * 0.04D;
					this.motY += d4 * 3.2D;
					this.motX -= vec3d.x * d4 / d0;
					this.motZ -= vec3d.z * d4 / d0;
				}

				if (d0 > 0.0D)
				{
					this.motX += (vec3d.x / d0 * d1 - this.motX) * 0.1D;
					this.motZ += (vec3d.z / d0 * d1 - this.motZ) * 0.1D;
				}

				this.motX *= 0.9900000095367432D;
				this.motY *= 0.9800000190734863D;
				this.motZ *= 0.9900000095367432D;
				this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
				if (this.positionChanged && !this.world.isClientSide)
				{
					d4 = Math.sqrt(this.motX * this.motX + this.motZ * this.motZ);
					double d5 = d1 - d4;
					float f8 = (float) (d5 * 10.0D - 3.0D);

					if (f8 > 0.0F)
					{
						this.a(this.m((int) f8), 1.0F, 1.0F);
						this.damageEntity(DamageSource.FLY_INTO_WALL, f8);
					}
				}

				if (this.onGround && !this.world.isClientSide)
					this.setFlag(7, false);
			}
			else
			{
				float f9 = 0.91F;
				BlockPosition.b blockposition_b = BlockPosition.b.d(this.locX, this.getBoundingBox().b - 1.0D, this.locZ);
				Throwable throwable = null;
				
				try
				{
					if (this.onGround)
						f9 = this.world.getType(blockposition_b).getBlock().n() * 0.91F;

					f4 = 0.16277137F / (f9 * f9 * f9);
					if (this.onGround)
						f3 = this.cK() * f4;
					else
						f3 = this.aU;

					this.a(f, f1, f2, f3);
					f9 = 0.91F;
					if (this.onGround)
						f9 = this.world.getType(blockposition_b.e(this.locX, this.getBoundingBox().b - 1.0D, this.locZ)).getBlock().n() * 0.91F;

					if (this.z_())
					{
						float f10 = 0.15F;

						this.motX = MathHelper.a(this.motX, -0.15000000596046448D, 0.15000000596046448D);
						this.motZ = MathHelper.a(this.motZ, -0.15000000596046448D, 0.15000000596046448D);
						this.fallDistance = 0.0F;
						if (this.motY < -0.15D)
							this.motY = -0.15D;
						
//						boolean flag = this.isSneaking() && this instanceof EntityHuman;
//						if (flag && this.motY < 0.0D)
//							this.motY = 0.0D;
					}

					this.move(EnumMoveType.SELF, this.motX, this.motY, this.motZ);
					if (this.positionChanged && this.z_())
						this.motY = 0.2D;

					if (this.hasEffect(MobEffects.LEVITATION))
					{
						this.motY += (0.05D * (double) (this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - this.motY) * 0.2D;
						this.fallDistance = 0.0F;
					}
					else
					{
						blockposition_b.e(this.locX, 0.0D, this.locZ);
						if (this.world.isClientSide && (!this.world.isLoaded(blockposition_b) || !this.world.getChunkAtWorldCoords(blockposition_b).y()))
						{
							if (this.locY > 0.0D)
								this.motY = -0.1D;
							else
								this.motY = 0.0D;
						}
//						else if (!this.isNoGravity())
//							this.motY -= d0;
					}

//					this.motY *= 0.9800000190734863D;
					this.motX *= (double) f9;
					this.motZ *= (double) f9;
				}
				catch (Throwable throwable1)
				{
					throwable = throwable1;
					throw throwable1;
				}
				finally
				{
					if (blockposition_b != null)
					{
						if (throwable != null)
							try
							{
								blockposition_b.close();
							}
							catch (Throwable throwable2)
							{
								throwable.addSuppressed(throwable2);
							}
						else
							blockposition_b.close();
					}
				}
			}
		}

		this.aF = this.aG;
		d2 = this.locX - this.lastX;
		d0 = this.locZ - this.lastZ;
		d1 = this instanceof EntityBird ? this.locY - this.lastY : 0.0D;
		float f10 = MathHelper.sqrt(d2 * d2 + d1 * d1 + d0 * d0) * 4.0F;

		if (f10 > 1.0F)
			f10 = 1.0F;

		this.aG += (f10 - this.aG) * 0.4F;
		this.aH += this.aG;
	}
}
