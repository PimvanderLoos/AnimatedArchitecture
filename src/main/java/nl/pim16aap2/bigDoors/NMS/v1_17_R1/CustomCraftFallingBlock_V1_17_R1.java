package nl.pim16aap2.bigDoors.NMS.v1_17_R1;

import nl.pim16aap2.bigDoors.NMS.CustomCraftFallingBlock_Vall;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class CustomCraftFallingBlock_V1_17_R1 extends CraftEntity implements FallingBlock, CustomCraftFallingBlock_Vall
{
    CustomCraftFallingBlock_V1_17_R1(final Server server, final CustomEntityFallingBlock_V1_17_R1 entity)
    {
        super((org.bukkit.craftbukkit.v1_17_R1.CraftServer) server, entity);
        setVelocity(new Vector(0, 0, 0));
        setDropItem(false);
        entity.P = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CustomEntityFallingBlock_V1_17_R1 getHandle()
    {
        return (CustomEntityFallingBlock_V1_17_R1) entity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOnGround()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "CraftFallingBlock";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EntityType getType()
    {
        return EntityType.FALLING_BLOCK;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public Material getMaterial()
    {
        return CraftMagicNumbers.getMaterial(getHandle().getBlock()).getItemType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getDropItem()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDropItem(final boolean drop)
    {
        getHandle().c = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHurtEntities()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHurtEntities(final boolean hurtEntities)
    {
        getHandle().ap = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTicksLived(final int value)
    {
        super.setTicksLived(value);

        // Second field for EntityFallingBlock
        getHandle().b = value;
    }

    @Override
    public void setHeadPose(EulerAngle pose)
    {
    }

    @Override
    public void setBodyPose(EulerAngle eulerAngle)
    {
    }

    @Override
    public byte getBlockData()
    {
        return -1;
    }

    @Override
    public int getBlockId()
    {
        return -1;
    }
}
