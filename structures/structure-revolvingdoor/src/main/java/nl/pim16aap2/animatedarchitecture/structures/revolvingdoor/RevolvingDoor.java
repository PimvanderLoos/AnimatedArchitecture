package nl.pim16aap2.animatedarchitecture.structures.revolvingdoor;

import lombok.EqualsAndHashCode;
import lombok.Locked;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.annotations.Deserialization;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureWithRotationPoint;
import nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes.IPerpetualMover;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.structures.bigdoor.BigDoor;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Revolving Door structure type.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class RevolvingDoor
    extends AbstractStructure
    implements IPerpetualMover, IStructureWithRotationPoint
{
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    @Deserialization
    public RevolvingDoor(BaseHolder base)
    {
        super(base, StructureTypeRevolvingDoor.get());
        this.lock = getLock();
    }

    @Override
    @Locked.Read("lock")
    protected double calculateAnimationCycleDistance()
    {
        return BigDoor.getMaxRadius(getCuboid(), getRotationPoint()) * Math.TAU;
    }

    @Override
    @Locked.Read("lock")
    protected Rectangle calculateAnimationRange()
    {
        final double maxRadius = BigDoor.getMaxRadius(getCuboid(), getRotationPoint());
        return BigDoor.calculateAnimationRange(maxRadius, getCuboid());
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
    @Locked.Read("lock")
    protected IAnimationComponent constructAnimationComponent(AnimationRequestData data)
    {
        return new RevolvingDoorAnimationComponent(data, getCurrentToggleDir());
    }
}
