package nl.pim16aap2.bigdoors.doors.portcullis;

import lombok.EqualsAndHashCode;
import lombok.ToString;
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
import nl.pim16aap2.bigdoors.util.RotateDirection;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;

/**
 * Represents a Portcullis doorType.
 *
 * @author Pim
 * @see DoorBase
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Portcullis extends AbstractDoor implements IDiscreteMovement, ITimerToggleable
{
    @EqualsAndHashCode.Exclude
    private static final DoorType DOOR_TYPE = DoorTypePortcullis.get();

    @PersistentVariable
    @GuardedBy("this")
    protected int blocksToMove;

    @PersistentVariable
    @GuardedBy("this")
    protected int autoCloseTime;

    @PersistentVariable
    @GuardedBy("this")
    protected int autoOpenTime;

    public Portcullis(DoorBase doorBase, int blocksToMove, int autoCloseTime, int autoOpenTime)
    {
        super(doorBase);
        this.blocksToMove = blocksToMove;
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
    }

    public Portcullis(DoorBase doorBase, int blocksToMove)
    {
        this(doorBase, blocksToMove, -1, -1);
    }

    @SuppressWarnings("unused")
    private Portcullis(DoorBase doorBase)
    {
        this(doorBase, -1); // Add tmp/default values
    }

    @Override
    protected synchronized double getLongestAnimationCycleDistance()
    {
        return blocksToMove;
    }

    @Override
    protected double calculateAnimationTime(double target)
    {
        return super.calculateAnimationTime(target + (isCurrentToggleDirUp() ? -0.2D : 0.2D));
    }

    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public RotateDirection getCurrentToggleDir()
    {
        synchronized (getDoorBase())
        {
            return isOpen() ? getOpenDir() : RotateDirection.getOpposite(getOpenDir());
        }
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        return Optional.of(getCuboid().move(0, getDirectedBlocksToMove(), 0));
    }

    /**
     * @return True if the current toggle dir goes up.
     */
    private boolean isCurrentToggleDirUp()
    {
        return getCurrentToggleDir() == RotateDirection.UP;
    }

    /**
     * @return The signed number of blocks to move (positive for up, negative for down).
     */
    private int getDirectedBlocksToMove()
    {
        return isCurrentToggleDirUp() ? getBlocksToMove() : -getBlocksToMove();
    }

    @Override
    protected synchronized BlockMover constructBlockMover(
        BlockMover.Context context, DoorActionCause cause, double time, boolean skipAnimation, Cuboid newCuboid,
        IPPlayer responsible, DoorActionType actionType)
        throws Exception
    {
        return new VerticalMover(
            context, this, time, skipAnimation, getDirectedBlocksToMove(),
            config.getAnimationSpeedMultiplier(getDoorType()), responsible, newCuboid, cause, actionType);
    }

    @Override
    public synchronized void setBlocksToMove(int blocksToMove)
    {
        this.blocksToMove = blocksToMove;
    }

    @Override
    public synchronized void setAutoCloseTime(int autoCloseTime)
    {
        this.autoCloseTime = autoCloseTime;
    }

    @Override
    public synchronized void setAutoOpenTime(int autoOpenTime)
    {
        this.autoOpenTime = autoOpenTime;
    }

    @Override
    public synchronized int getBlocksToMove()
    {
        return this.blocksToMove;
    }

    @Override
    public synchronized int getAutoCloseTime()
    {
        return this.autoCloseTime;
    }

    @Override
    public synchronized int getAutoOpenTime()
    {
        return this.autoOpenTime;
    }
}
