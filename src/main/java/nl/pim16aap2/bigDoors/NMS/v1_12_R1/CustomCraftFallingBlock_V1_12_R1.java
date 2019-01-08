package nl.pim16aap2.bigDoors.NMS.v1_12_R1;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;

public class CustomCraftFallingBlock_V1_12_R1 extends CraftEntity implements FallingBlock, CustomCraftFallingBlock_Vall
{

    public CustomCraftFallingBlock_V1_12_R1(Server server, CustomEntityFallingBlock_V1_12_R1 entity)
    {
        super((org.bukkit.craftbukkit.v1_12_R1.CraftServer) server, entity);
		setVelocity(new Vector(0, 0, 0));
		setDropItem(false);
    }

    @Override
    public CustomEntityFallingBlock_V1_12_R1 getHandle()
    {
        return (CustomEntityFallingBlock_V1_12_R1) entity;
    }

    @Override
    public boolean isOnGround()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "CraftFallingBlock. E.ID = " + entity.getId();
    }

    @Override
    public EntityType getType()
    {
        return EntityType.FALLING_BLOCK;
    }

    @Override
    @SuppressWarnings("deprecation")
	public Material getMaterial()
    {
        return Material.getMaterial(getBlockId());
    }

    @Override
    public int getBlockId()
    {
    		System.out.println("CustomFallingBlock.GetBlockID() MUST NOT BE USED!");
    		return -1;
    }

    @Override
    public byte getBlockData()
    {
        return (byte) getHandle().getBlock().getBlock().toLegacyData(getHandle().getBlock());
    }

    @Override
    public boolean getDropItem()
    {
        return getHandle().dropItem;
    }

    @Override
    public void setDropItem(boolean drop)
    {
        getHandle().dropItem = drop;
    }

    @Override
    public boolean canHurtEntities()
    {
        return getHandle().hurtEntities;
    }

    @Override
    public void setHurtEntities(boolean hurtEntities)
    {
        getHandle().hurtEntities = hurtEntities;
    }

    @Override
    public void setTicksLived(int value)
    {
        super.setTicksLived(value);

        // Second field for EntityFallingBlock
        getHandle().ticksLived = value;
    }

	@Override
	public Spigot spigot()
	{
		return null;
	}

	@Override
	public void setHeadPose(EulerAngle pose) {}

	@Override
	public void setBodyPose(EulerAngle eulerAngle) {}
}

