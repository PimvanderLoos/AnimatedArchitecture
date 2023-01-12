package nl.pim16aap2.bigdoors.doors.slidingdoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.IDiscreteMovement;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Sliding Door doorType.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SlidingDoor extends AbstractDoor implements IDiscreteMovement, ITimerToggleable
{
    private static final DoorType DOOR_TYPE = DoorTypeSlidingDoor.get();

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

    public SlidingDoor(DoorBase doorBase, int blocksToMove, int autoCloseTime, int autoOpenTime)
    {
        super(doorBase);
        this.lock = getLock();
        this.blocksToMove = blocksToMove;
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
    }

    public SlidingDoor(DoorBase doorBase, int blocksToMove)
    {
        this(doorBase, blocksToMove, -1, -1);
    }

    @SuppressWarnings("unused")
    private SlidingDoor(DoorBase doorBase)
    {
        this(doorBase, -1); // Add tmp/default values
    }

    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    @Locked.Read
    protected double getLongestAnimationCycleDistance()
    {
        return blocksToMove;
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
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    @Locked.Read
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final Vector3Di vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));
        return Optional.of(getCuboid().move(0, getBlocksToMove() * vec.y(), 0));
    }

    @Override
    @Locked.Read
    protected BlockMover constructBlockMover(
        BlockMover.Context context, DoorActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible,
        DoorActionType actionType)
        throws Exception
    {
        return new SlidingMover(
            context, this, time, skipAnimation, getBlocksToMove(), getCurrentToggleDir(),
            config.getAnimationSpeedMultiplier(getDoorType()), responsible, newCuboid, cause, actionType);
    }
}
