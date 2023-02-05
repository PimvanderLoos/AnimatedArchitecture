package nl.pim16aap2.bigdoors.structures.revolvingdoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureRequestData;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structures.bigdoor.BigDoor;
import nl.pim16aap2.bigdoors.core.structures.serialization.Deserialization;
import nl.pim16aap2.bigdoors.core.structures.serialization.PersistentVariable;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MathUtil;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Rectangle;

import javax.annotation.concurrent.GuardedBy;
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
public class RevolvingDoor extends AbstractStructure
{
    @EqualsAndHashCode.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    /**
     * The number of quarter circles (so 90 degree rotations) this structure will make before stopping.
     *
     * @return The number of quarter circles this structure will rotate.
     */
    @PersistentVariable("quarterCircles")
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private int quarterCircles;

    @Deserialization
    public RevolvingDoor(
        BaseHolder base,
        @PersistentVariable("quarterCircles") int quarterCircles)
    {
        super(base, StructureTypeRevolvingDoor.get());
        this.lock = getLock();
        this.quarterCircles = quarterCircles;
    }

    public RevolvingDoor(BaseHolder base)
    {
        this(base, 1);
    }

    @Override
    @Locked.Read
    protected double calculateAnimationCycleDistance()
    {
        return BigDoor.getMaxRadius(getCuboid(), getRotationPoint()) * MathUtil.HALF_PI;
    }

    @Override
    @Locked.Read
    protected Rectangle calculateAnimationRange()
    {
        final double maxRadius = BigDoor.getMaxRadius(getCuboid(), getRotationPoint());
        return BigDoor.calculateAnimationRange(maxRadius, getCuboid());
    }

    @Override
    @Locked.Read
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final MovementDirection movementDirection = getCurrentToggleDir();
        final double angle = movementDirection == MovementDirection.CLOCKWISE ? MathUtil.HALF_PI :
                             movementDirection == MovementDirection.COUNTERCLOCKWISE ? -MathUtil.HALF_PI : 0.0D;
        if (angle == 0.0D)
        {
            log.atSevere()
               .log("Invalid movement direction '%s' for revolving door: %d", movementDirection.name(), getUid());
            return Optional.empty();
        }

        return Optional.of(getCuboid().updatePositions(vec -> vec.rotateAroundYAxis(getRotationPoint(), angle)));
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
        return new RevolvingDoorAnimationComponent(data, getCurrentToggleDir(), quarterCircles);
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
