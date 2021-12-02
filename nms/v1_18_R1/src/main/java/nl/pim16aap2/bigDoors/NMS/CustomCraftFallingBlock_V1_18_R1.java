package nl.pim16aap2.bigDoors.NMS;

import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

public class CustomCraftFallingBlock_V1_18_R1 extends CraftEntity implements FallingBlock, CustomCraftFallingBlock
{
    CustomCraftFallingBlock_V1_18_R1(final Server server, final CustomEntityFallingBlock_V1_18_R1 entity)
    {
        super((org.bukkit.craftbukkit.v1_18_R1.CraftServer) server, entity);
        setVelocity(new Vector(0, 0, 0));
        setDropItem(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CustomEntityFallingBlock_V1_18_R1 getHandle()
    {
        return (CustomEntityFallingBlock_V1_18_R1) entity;
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
        return CraftMagicNumbers.getMaterial(getHandle().i()).getItemType();
    }

    @Override
    public BlockData getBlockData()
    {
        throw new IllegalStateException("Trying to access getBlockData is not possible for custom falling blocks!");
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
        getHandle().ar = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTicksLived(final int value)
    {
        super.setTicksLived(value);
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
}
