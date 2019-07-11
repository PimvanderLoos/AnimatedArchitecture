package nl.pim16aap2.bigdoors.spigot.spigot_v1_14_R1;

import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_14_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

/**
 * V1_14_R1 implementation of {@link ICustomCraftFallingBlock}.
 *
 * @author Pim
 * @see ICustomCraftFallingBlock
 */
public class CustomCraftFallingBlock_V1_14_R1 extends CraftEntity implements FallingBlock, ICustomCraftFallingBlock
{
    public CustomCraftFallingBlock_V1_14_R1(Server server, CustomEntityFallingBlock_V1_14_R1 entity)
    {
        super((org.bukkit.craftbukkit.v1_14_R1.CraftServer) server, entity);
        setVelocity(new Vector(0, 0, 0));
        setDropItem(false);
    }

    @Override
    public CustomEntityFallingBlock_V1_14_R1 getHandle()
    {
        return (CustomEntityFallingBlock_V1_14_R1) entity;
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
    @Deprecated
    public Material getMaterial()
    {
        return CraftMagicNumbers.getMaterial(getHandle().getBlock()).getItemType();
    }

    @Override
    public BlockData getBlockData()
    {
        return CraftBlockData.fromData(getHandle().getBlock());
    }

    @Override
    public boolean getDropItem()
    {
        return false;
    }

    @Override
    public void setDropItem(boolean drop)
    {
        getHandle().dropItem = false;
    }

    @Override
    public boolean canHurtEntities()
    {
        return false;
    }

    @Override
    public void setHurtEntities(boolean hurtEntities)
    {
        getHandle().hurtEntities = false;
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

    /**
     * {@inheritDoc}
     *
     * @deprecated Not currently implemented.
     */
    @Deprecated
    @Override
    public void setHeadPose(EulerAngle pose)
    {
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Not currently implemented.
     */
    @Deprecated
    @Override
    public void setBodyPose(EulerAngle eulerAngle)
    {
    }
}
