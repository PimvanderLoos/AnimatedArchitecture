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
import org.jetbrains.annotations.NotNull;

/**
 * V1_14_R1 implementation of {@link ICustomCraftFallingBlock}.
 *
 * @author Pim
 * @see ICustomCraftFallingBlock
 */
public class CustomCraftFallingBlock_V1_14_R1 extends CraftEntity implements FallingBlock, ICustomCraftFallingBlock
{
    CustomCraftFallingBlock_V1_14_R1(final @NotNull Server server,
                                     final @NotNull CustomEntityFallingBlock_V1_14_R1 entity)
    {
        super((org.bukkit.craftbukkit.v1_14_R1.CraftServer) server, entity);
        setVelocity(new Vector(0, 0, 0));
        setDropItem(false);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public CustomEntityFallingBlock_V1_14_R1 getHandle()
    {
        return (CustomEntityFallingBlock_V1_14_R1) entity;
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
    @NotNull
    @Override
    public EntityType getType()
    {
        return EntityType.FALLING_BLOCK;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    @Deprecated
    public Material getMaterial()
    {
        return CraftMagicNumbers.getMaterial(getHandle().getBlock()).getItemType();
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public BlockData getBlockData()
    {
        return CraftBlockData.fromData(getHandle().getBlock());
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
        getHandle().dropItem = false;
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
        getHandle().hurtEntities = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTicksLived(final int value)
    {
        super.setTicksLived(value);

        // Second field for EntityFallingBlock
        getHandle().ticksLived = value;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Not currently implemented.
     */
    @Deprecated
    @Override
    public void setHeadPose(final @NotNull EulerAngle pose)
    {
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated Not currently implemented.
     */
    @Deprecated
    @Override
    public void setBodyPose(final @NotNull EulerAngle eulerAngle)
    {
    }
}
