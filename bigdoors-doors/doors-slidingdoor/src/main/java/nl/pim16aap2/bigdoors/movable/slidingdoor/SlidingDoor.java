package nl.pim16aap2.bigdoors.movable.slidingdoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IDiscreteMovement;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.Rectangle;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Sliding Door movable type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SlidingDoor extends AbstractMovable implements IDiscreteMovement, ITimerToggleable
{
    private static final MovableType MOVABLE_TYPE = MovableSlidingDoor.get();

    @EqualsAndHashCode.Exclude
    private final ReentrantReadWriteLock lock;

    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    protected int blocksToMove;

    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    protected int autoCloseTime;

    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    @Setter(onMethod_ = @Locked.Write)
    protected int autoOpenTime;

    public SlidingDoor(MovableBase base, int blocksToMove, int autoCloseTime, int autoOpenTime)
    {
        super(base);
        this.lock = getLock();
        this.blocksToMove = blocksToMove;
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
    }

    public SlidingDoor(MovableBase base, int blocksToMove)
    {
        this(base, blocksToMove, -1, -1);
    }

    @SuppressWarnings("unused")
    private SlidingDoor(MovableBase base)
    {
        this(base, -1); // Add tmp/default values
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }

    @Override
    @Locked.Read
    protected double getLongestAnimationCycleDistance()
    {
        return blocksToMove;
    }

    @Override
    @Locked.Read
    public Rectangle getAnimationRange()
    {
        final Cuboid cuboid = getCuboid();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        final Cuboid cuboidRange = switch (getCurrentToggleDir())
            {
                case NORTH -> new Cuboid(min.add(0, 0, -blocksToMove), max.add(0, 0, 0)); // -z
                case EAST -> new Cuboid(min.add(0, 0, 0), max.add(blocksToMove, 0, 0)); // +x
                case SOUTH -> new Cuboid(min.add(0, 0, 0), max.add(0, 0, blocksToMove)); // +z
                case WEST -> new Cuboid(min.add(-blocksToMove, 0, 0), max.add(0, 0, 0)); // -x
                default -> cuboid.grow(blocksToMove, 0, blocksToMove);
            };
        return cuboidRange.asFlatRectangle();
    }

    @Override
    public boolean canSkipAnimation()
    {
        return true;
    }

    @Override
    public RotateDirection cycleOpenDirection()
    {
        final RotateDirection openDirection = getOpenDir();
        return openDirection.equals(RotateDirection.NORTH) ? RotateDirection.EAST :
               openDirection.equals(RotateDirection.EAST) ? RotateDirection.SOUTH :
               openDirection.equals(RotateDirection.SOUTH) ? RotateDirection.WEST : RotateDirection.NORTH;
    }

    @Override
    @Locked.Read
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? getOpenDir() : RotateDirection.getOpposite(getOpenDir());
    }

    @Override
    @Locked.Read
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final Vector3Di vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));
        return Optional.of(getCuboid().move(getBlocksToMove() * vec.x(), 0, getBlocksToMove() * vec.z()));
    }

    @Override
    @Locked.Read
    protected BlockMover constructBlockMover(
        BlockMover.Context context, MovableSnapshot movableSnapshot, MovableActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible, MovableActionType actionType)
        throws Exception
    {
        return new SlidingMover(
            context, this, movableSnapshot, time, skipAnimation, getBlocksToMove(), getCurrentToggleDir(),
            responsible, newCuboid, cause, actionType);
    }
}
