package nl.pim16aap2.bigDoors.NMS;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class CustomCraftFallingBlock_V1_13_R1 extends CraftEntity implements FallingBlock, CustomCraftFallingBlock
{

    public CustomCraftFallingBlock_V1_13_R1(Server server, CustomEntityFallingBlock_V1_13_R1 entity)
    {
        super((org.bukkit.craftbukkit.v1_13_R1.CraftServer) server, entity);
        setVelocity(new Vector(0, 0, 0));
        setDropItem(false);
    }

    @Override
    public CustomEntityFallingBlock_V1_13_R1 getHandle()
    {
        return (CustomEntityFallingBlock_V1_13_R1) entity;
    }

    @Override
    public boolean isOnGround()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "CraftFallingBlock";
    }

    @Override
    public EntityType getType()
    {
        return EntityType.FALLING_BLOCK;
    }

    @Override
    public Material getMaterial()
    {
        System.out.println("CustomFallingBlock.getMaterial() must not be used!");
        return null;
    }

    @Override
    public BlockData getBlockData()
    {
        throw new IllegalStateException("Trying to access getBlockData is not possible for custom falling blocks!");
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

