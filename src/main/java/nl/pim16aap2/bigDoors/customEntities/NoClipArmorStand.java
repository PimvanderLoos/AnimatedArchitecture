package nl.pim16aap2.bigDoors.customEntities;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftEntity;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import net.minecraft.server.v1_11_R1.Entity;

public class NoClipArmorStand extends net.minecraft.server.v1_11_R1.EntityArmorStand
{
	public boolean isNoGravity;

	public NoClipArmorStand(World world, Location loc)
	{
		super(((CraftWorld) world).getHandle());
		this.setPosition(loc.getX(), loc.getY(), loc.getZ());
		this.noclip = true;
		this.isNoGravity = false;
	}

	@Override
	public void a(float f, float f1, float f2)
	{
		super.a(f, f1, f2);
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
		} else
		{
			return false;
		}
	}

	public boolean eject()
	{
		if (isEmpty())
		{
			return false;
		}

		getHandle().az(); // PAIL: rename
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
		return Lists
				.newArrayList(Lists.transform(getHandle().passengers, new Function<Entity, org.bukkit.entity.Entity>()
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

	@Override
	public void g(float f, float f1)
	{
		if (!this.isNoGravity())
		{
			super.g(f, f1);
		} else
		{
			super.g(f, f1);
		}
	}
}
