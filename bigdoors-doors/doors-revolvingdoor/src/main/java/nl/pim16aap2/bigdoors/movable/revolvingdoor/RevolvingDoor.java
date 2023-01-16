package nl.pim16aap2.bigdoors.movable.revolvingdoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Locked;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.movable.bigdoor.BigDoor;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MathUtil;
import nl.pim16aap2.bigdoors.util.RotateDirection;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

/**
 * Represents a Revolving Door doorType.
 *
 * @author Pim
 * @see MovableBase
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Flogger
public class RevolvingDoor extends AbstractMovable
{
    private static final MovableType MOVABLE_TYPE = MovableRevolvingDoor.get();

    @EqualsAndHashCode.Exclude
    private final ReentrantReadWriteLock lock;

    @Getter
    private final double longestAnimationCycleDistance;

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

    public RevolvingDoor(MovableBase doorBase, int quarterCircles)
    {
        super(doorBase);
        this.lock = getLock();
        this.quarterCircles = quarterCircles;
        this.longestAnimationCycleDistance =
            BigDoor.calculateLongestAnimationCycleDistance(getCuboid(), getRotationPoint());
    }

    public RevolvingDoor(MovableBase doorBase)
    {
        this(doorBase, 1);
    }

    @Override
    public MovableType getMovableType()
    {
        return MOVABLE_TYPE;
    }

    @Override
    @Locked.Read
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final RotateDirection rotateDirection = getCurrentToggleDir();
        final double angle = rotateDirection == RotateDirection.CLOCKWISE ? MathUtil.HALF_PI :
                             rotateDirection == RotateDirection.COUNTERCLOCKWISE ? -MathUtil.HALF_PI : 0.0D;
        if (angle == 0.0D)
        {
            log.at(Level.SEVERE)
               .log("Invalid open direction '%s' for revolving door: %d", rotateDirection.name(), getMovableUID());
            return Optional.empty();
        }

        return Optional.of(getCuboid().updatePositions(vec -> vec.rotateAroundYAxis(getRotationPoint(), angle)));
    }

    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }

    @Override
    @Locked.Read
    protected BlockMover constructBlockMover(
        BlockMover.Context context, MovableSnapshot movableSnapshot, MovableActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        MovableActionType actionType)
        throws Exception
    {
        return new RevolvingDoorMover(
            context, this, movableSnapshot, time, config.getAnimationSpeedMultiplier(getMovableType()),
            getCurrentToggleDir(),
            responsible, quarterCircles, cause, newCuboid, actionType);
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
