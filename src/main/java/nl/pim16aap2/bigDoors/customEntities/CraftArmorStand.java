package nl.pim16aap2.bigDoors.customEntities;

import net.minecraft.server.v1_11_R1.EntityLiving;

import org.bukkit.craftbukkit.v1_11_R1.entity.CraftLivingEntity;

public class CraftArmorStand extends CraftLivingEntity
{
	public CraftArmorStand(org.bukkit.craftbukkit.v1_11_R1.CraftServer server, EntityLiving entity)
	{
		super(server, entity);
	}
	
    public void recalcPosition() 
    {
        entity.recalcPosition();
    }

	@Override
	public void setGravity(boolean gravity)
	{
		super.setGravity(gravity);
		getHandle().noclip = gravity;
	}
}
