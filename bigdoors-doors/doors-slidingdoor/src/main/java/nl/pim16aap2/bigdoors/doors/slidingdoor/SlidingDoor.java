package nl.pim16aap2.bigdoors.doors.slidingdoor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpeningUtility;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IDiscreteMovement;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents a Sliding Door doorType.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SlidingDoor extends AbstractDoor implements IDiscreteMovement, ITimerToggleable
{
    @EqualsAndHashCode.Exclude
    private static final @NotNull DoorType DOOR_TYPE = DoorTypeSlidingDoor.get();

    @Getter
    @Setter
    @PersistentVariable
    protected int blocksToMove;

    @Getter
    @Setter
    @PersistentVariable
    protected int autoCloseTime;

    @Getter
    @Setter
    @PersistentVariable
    protected int autoOpenTime;

    public SlidingDoor(final @NotNull DoorBase doorBase, final int blocksToMove, final int autoCloseTime,
                       final int autoOpenTime)
    {
        super(doorBase);
        this.blocksToMove = blocksToMove;
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
    }

    public SlidingDoor(final @NotNull DoorBase doorBase, final int blocksToMove)
    {
        this(doorBase, blocksToMove, -1, -1);
    }

    @SuppressWarnings("unused")
    private SlidingDoor(final @NotNull DoorBase doorBase)
    {
        this(doorBase, -1); // Add tmp/default values
    }

    @Override
    public @NotNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public boolean canSkipAnimation()
    {
        return true;
    }

    @Override
    public @NotNull RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.EAST :
               getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.SOUTH :
               getOpenDir().equals(RotateDirection.SOUTH) ? RotateDirection.WEST : RotateDirection.NORTH;
    }

    @Override
    public synchronized @NotNull RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    public synchronized @NotNull Optional<Cuboid> getPotentialNewCoordinates()
    {
        final @NotNull Vector3Di vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));
        return Optional.of(getCuboid().move(0, getBlocksToMove() * vec.y(), 0));
    }

    @Override
    protected @NotNull BlockMover constructBlockMover(final @NotNull DoorActionCause cause, final double time,
                                                      final boolean skipAnimation, final @NotNull Cuboid newCuboid,
                                                      final @NotNull IPPlayer responsible,
                                                      final @NotNull DoorActionType actionType)
        throws Exception
    {
        final @NotNull RotateDirection currentToggleDir = getCurrentToggleDir();
        return new SlidingMover(this, time, skipAnimation, getBlocksToMove(), currentToggleDir,
                                DoorOpeningUtility.getMultiplier(this), responsible, newCuboid, cause, actionType);
    }
}
