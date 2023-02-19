package nl.pim16aap2.bigdoors.structures.revolvingdoor;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.annotations.Deserialization;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureRequestData;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.structurearchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Rectangle;
import nl.pim16aap2.bigdoors.structures.bigdoor.BigDoor;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Revolving Door structure type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class RevolvingDoor extends AbstractStructure implements IPerpetualMover
{
    @EqualsAndHashCode.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    @Deserialization
    public RevolvingDoor(BaseHolder base)
    {
        super(base, StructureTypeRevolvingDoor.get());
        this.lock = getLock();
    }

    @Override
    @Locked.Read
    protected double calculateAnimationCycleDistance()
    {
        return BigDoor.getMaxRadius(getCuboid(), getRotationPoint()) * Math.TAU;
    }

    @Override
    @Locked.Read
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
    @Locked.Read
    protected IAnimationComponent constructAnimationComponent(StructureRequestData data)
    {
        return new RevolvingDoorAnimationComponent(data, getCurrentToggleDir());
    }

    @Override
    public boolean isOpenable()
    {
        return true;
    }

    @Override
    public boolean isCloseable()
    {
        return true;
    }
}
