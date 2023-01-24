package nl.pim16aap2.bigdoors.movable.revolvingdoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.bigdoor.BigDoor;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MathUtil;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Rectangle;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Revolving Door movable type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class RevolvingDoor extends AbstractMovable
{
    private static final MovableType MOVABLE_TYPE = MovableRevolvingDoor.get();

    @EqualsAndHashCode.Exclude
    private final ReentrantReadWriteLock lock;

    /**
     * The number of quarter circles (so 90 degree rotations) this movable will make before stopping.
     *
     * @return The number of quarter circles this movable will rotate.
     */
    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    private int quarterCircles;

    public RevolvingDoor(AbstractMovable.MovableBaseHolder base, int quarterCircles)
    {
        super(base);
        this.lock = getLock();
        this.quarterCircles = quarterCircles;
    }

    public RevolvingDoor(AbstractMovable.MovableBaseHolder base)
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
    public MovableType getType()
    {
        return MOVABLE_TYPE;
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
    protected BlockMover constructBlockMover(MovementRequestData data)
        throws Exception
    {
        return new RevolvingDoorMover(this, data, getCurrentToggleDir(), quarterCircles);
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
