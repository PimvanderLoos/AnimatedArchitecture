package nl.pim16aap2.bigDoors.customEntities;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftEntity;

public class CustomCraftFallingBlock extends CraftEntity implements FallingBlock 
{

    public CustomCraftFallingBlock(Server server, CustomEntityFallingBlock entity) 
    {
        super((org.bukkit.craftbukkit.v1_11_R1.CraftServer) server, entity);
		this.setVelocity(new Vector(0, 0, 0));
		this.setDropItem(false);
    }

    @Override
    public CustomEntityFallingBlock getHandle() 
    {
        return (CustomEntityFallingBlock) entity;
    }
    
    public boolean isOnGround() 
    {
        return false;
    }

    @Override
    public String toString() 
    {
        return "CraftFallingBlock";
    }

    public EntityType getType() 
    {
        return EntityType.FALLING_BLOCK;
    }

    @SuppressWarnings("deprecation")
	public Material getMaterial() 
    {
        return Material.getMaterial(getBlockId());
    }

    public int getBlockId() 
    {
    		System.out.println("CustomFallingBlockGetBlockID() MUST NOT BE USED!");
    		return -1;
    }

    public byte getBlockData() 
    {
        return (byte) getHandle().getBlock().getBlock().toLegacyData(getHandle().getBlock());
    }

    public boolean getDropItem() 
    {
        return getHandle().dropItem;
    }

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
}

