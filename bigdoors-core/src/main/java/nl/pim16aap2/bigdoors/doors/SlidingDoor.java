package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeSlidingDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.SlidingMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Sliding Door doorType.
 *
 * @author Pim
 */
public class SlidingDoor extends AbstractDoorBase
    implements IStationaryDoorArchetype, IBlocksToMoveArchetype, ITimerToggleableArchetype
{
    private static final DoorType DOOR_TYPE = DoorTypeSlidingDoor.get();

    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int blocksToMove;

    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int autoCloseTime;

    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int autoOpenTime;

    public SlidingDoor(final @NotNull DoorData doorData, final int blocksToMove, final int autoCloseTime,
                       final int autoOpenTime)
    {
        super(doorData);
        this.blocksToMove = blocksToMove;
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public Vector2Di[] calculateChunkRange()
    {
        int distanceX = 0;
        int distanceZ = 0;
        if (getOpenDir().equals(RotateDirection.NORTH) || getOpenDir().equals(RotateDirection.SOUTH))
            distanceZ = (getBlocksToMove() > 0 ? Math.max(dimensions.getZ(), getBlocksToMove()) :
                         Math.min(-dimensions.getZ(), getBlocksToMove())) / 16 + 1;
        else
            distanceX = (getBlocksToMove() > 0 ? Math.max(dimensions.getX(), getBlocksToMove()) :
                         Math.min(-dimensions.getX(), getBlocksToMove())) / 16 + 1;

        return new Vector2Di[]{new Vector2Di(getChunk().getX() - distanceX, getChunk().getY() - distanceZ),
                               new Vector2Di(getChunk().getX() + distanceX, getChunk().getY() + distanceZ)};
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public RotateDirection getDefaultOpenDirection()
    {
        return RotateDirection.NORTH;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.NORTH) ? RotateDirection.EAST :
               getOpenDir().equals(RotateDirection.EAST) ? RotateDirection.SOUTH :
               getOpenDir().equals(RotateDirection.SOUTH) ? RotateDirection.WEST : RotateDirection.NORTH;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        IVector3DiConst vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));

        int blocksToMove = getBlocksToMove() > 0 ? getBlocksToMove() :
                           1 + Math.abs(vec.getX() * dimensions.getX() + vec.getZ() * dimensions.getZ());

        newMin.setX(minimum.getX() + blocksToMove * vec.getX());
        newMin.setY(minimum.getY());
        newMin.setZ(minimum.getZ() + blocksToMove * vec.getZ());

        newMax.setX(maximum.getX() + blocksToMove * vec.getX());
        newMax.setY(maximum.getY());
        newMax.setZ(maximum.getZ() + blocksToMove * vec.getZ());
        return true;
    }

    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull IVector3DiConst newMin,
                                      final @NotNull IVector3DiConst newMax, final @NotNull IPPlayer responsible,
                                      final @NotNull DoorActionType actionType)
    {
        RotateDirection currentToggleDir = getCurrentToggleDir();
        int finalBlocksToMove =
            (currentToggleDir.equals(RotateDirection.NORTH) || currentToggleDir.equals(RotateDirection.SOUTH)) ?
            newMin.getZ() - minimum.getZ() : newMin.getX() - minimum.getX();

        doorOpeningUtility.registerBlockMover(
            new SlidingMover(this, time, skipAnimation, finalBlocksToMove, currentToggleDir,
                             doorOpeningUtility.getMultiplier(this), responsible, newMin, newMax, cause, actionType));
    }

    @Override
    public boolean equals(@Nullable Object o)
    {
        if (!super.equals(o))
            return false;
        if (getClass() != o.getClass())
            return false;

        final @NotNull SlidingDoor other = (SlidingDoor) o;
        return blocksToMove == other.blocksToMove;
    }
}
