package nl.pim16aap2.bigdoors.spigot.v1_15_R1;

import lombok.EqualsAndHashCode;
import net.minecraft.server.v1_15_R1.Vec3D;
import nl.pim16aap2.bigdoors.api.ICustomCraftFallingBlock;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.util.vector.Vector3Dd;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_15_R1.util.CraftMagicNumbers;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * V1_15_R1 implementation of {@link ICustomCraftFallingBlock}.
 *
 * @author Pim
 * @see ICustomCraftFallingBlock
 */
@EqualsAndHashCode(callSuper = true)
public class CustomCraftFallingBlock_V1_15_R1 extends CraftEntity implements FallingBlock, ICustomCraftFallingBlock
{
    // field entity already exists in CraftEntity, but we want to override it on purpose.
    @SuppressWarnings("squid:S2387")
    protected final CustomEntityFallingBlock_V1_15_R1 entity;

    CustomCraftFallingBlock_V1_15_R1(Server server,
                                     nl.pim16aap2.bigdoors.spigot.v1_15_R1.CustomEntityFallingBlock_V1_15_R1 entity)
    {
        super((org.bukkit.craftbukkit.v1_15_R1.CraftServer) server, entity);
        this.entity = entity;
        setVelocity(new Vector(0, 0, 0));
        setDropItem(false);
        entity.noclip = true;
    }

    // TODO: It should apply velocity if possible, but the issue is that the last position isn't the actual last
    //       position, because the velocity moved it. or does the tp offset it?
    // TODO: The blocks should lag behind 1 tick, so they have 3 variables: LastPos, CurrentPos, FuturePos.
    //       This can be used to set proper velocity as well.
    @Override
    public boolean teleport(Vector3Dd newPosition, Vector3Dd rotation, TeleportMode teleportMode)
    {
        return entity.teleport(newPosition, rotation);
    }

    @Override
    public void setVelocity(Vector3Dd vector)
    {
        entity.setMot(new Vec3D(vector.x(), vector.y(), vector.z()));
        entity.velocityChanged = true;
    }

    @Override
    public IPLocation getPLocation()
    {
        return SpigotAdapter.wrapLocation(super.getLocation());
    }

    @Override
    public Vector3Dd getPosition()
    {
        return entity.getCurrentPosition();
    }

    @Override
    public Vector3Dd getPVelocity()
    {
        final Vector bukkitVelocity = super.getVelocity();
        return new Vector3Dd(bukkitVelocity.getX(), bukkitVelocity.getY(), bukkitVelocity.getZ());
    }

    @Override
    public nl.pim16aap2.bigdoors.spigot.v1_15_R1.CustomEntityFallingBlock_V1_15_R1 getHandle()
    {
        return entity;
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
    public @NotNull EntityType getType()
    {
        return EntityType.FALLING_BLOCK;
    }

    @Override
    @Deprecated
    // Deprecated tag doesn't exist here (1123), but it already exists in the super class.
    // We do not want to remove this deprecated code (1133) because we don't own it and _have_ to override it.
    @SuppressWarnings({"squid:S1123", "squid:S1133"})
    public @NotNull Material getMaterial()
    {
        return CraftMagicNumbers.getMaterial(entity.getBlock()).getItemType();
    }

    @Override
    public @NotNull BlockData getBlockData()
    {
        return CraftBlockData.fromData(entity.getBlock());
    }

    @Override
    public boolean getDropItem()
    {
        return false;
    }

    @Override
    public void setDropItem(boolean drop)
    {
        // ignored
    }

    @Override
    public boolean canHurtEntities()
    {
        return false;
    }

    @Override
    public void setHurtEntities(boolean hurtEntities)
    {
        // ignored
    }

    @Override
    public void setTicksLived(int value)
    {
        super.setTicksLived(value);

        // Second field for EntityFallingBlock
        entity.setTicksLived(value);
    }
}
