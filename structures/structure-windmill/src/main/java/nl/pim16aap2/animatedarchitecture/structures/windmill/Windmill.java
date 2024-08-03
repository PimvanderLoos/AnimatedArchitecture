package nl.pim16aap2.animatedarchitecture.structures.windmill;

import lombok.EqualsAndHashCode;
import lombok.Locked;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.annotations.Deserialization;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureWithRotationPoint;
import nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes.IPerpetualMover;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.structures.drawbridge.Drawbridge;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Windmill structure type.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Windmill
    extends AbstractStructure
    implements IHorizontalAxisAligned, IPerpetualMover, IStructureWithRotationPoint
{
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    @Deserialization
    public Windmill(BaseHolder base)
    {
        super(base, StructureTypeWindmill.get());
        this.lock = getLock();
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        return Optional.of(getCuboid());
    }

    @Override
    public MovementDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    @Override
    public boolean isNorthSouthAnimated()
    {
        final MovementDirection openDir = getOpenDir();
        return openDir == MovementDirection.NORTH || openDir == MovementDirection.SOUTH;
    }

    @Override
    @Locked.Read("lock")
    protected double calculateAnimationCycleDistance()
    {
        return Drawbridge.getMaxRadius(isNorthSouthAnimated(), getCuboid(), getRotationPoint()) * Math.TAU;
    }

    @Override
    @Locked.Read("lock")
    protected Rectangle calculateAnimationRange()
    {
        final double maxRadius = Drawbridge.getMaxRadius(isNorthSouthAnimated(), getCuboid(), getRotationPoint());
        return Drawbridge.calculateAnimationRange(maxRadius, getCuboid());
    }

    @Override
    public MovementDirection getCycledOpenDirection()
    {
        final MovementDirection openDir = getOpenDir();
        return openDir.equals(MovementDirection.NORTH) ? MovementDirection.EAST :
            openDir.equals(MovementDirection.EAST) ? MovementDirection.SOUTH :
                openDir.equals(MovementDirection.SOUTH) ? MovementDirection.WEST :
                    MovementDirection.NORTH;
    }

    @Override
    @Locked.Read("lock")
    protected IAnimationComponent constructAnimationComponent(AnimationRequestData data)
    {
        return new WindmillAnimationComponent(data, getCurrentToggleDir(), isNorthSouthAnimated());
    }
}
