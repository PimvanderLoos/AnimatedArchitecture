package nl.pim16aap2.bigdoors.doors.portcullis;

import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Portcullis doorType.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public class Portcullis extends AbstractDoorBase
    implements IMovingDoorArchetype, IBlocksToMoveArchetype, ITimerToggleableArchetype
{
    @NotNull
    private static final DoorType DOOR_TYPE = DoorTypePortcullis.get();

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int blocksToMove;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int autoCloseTime;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int autoOpenTime;

    public Portcullis(final @NotNull DoorData doorData, final int blocksToMove, final int autoCloseTime,
                      final int autoOpenTime)
    {
        super(doorData);
        this.blocksToMove = blocksToMove;
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
    }

    public Portcullis(final @NotNull DoorData doorData, final int blocksToMove)
    {
        this(doorData, blocksToMove, -1, -1);
    }

    @Override
    public @NotNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public @NotNull RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.UP) ? RotateDirection.DOWN : RotateDirection.UP;
    }

    @Override
    public @NotNull Vector2Di[] calculateChunkRange()
    {
        return calculateCurrentChunkRange();
    }

    @Override
    public @NotNull RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.DOWN : RotateDirection.UP;
    }

    @Override
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        Vector3DiConst vec = PBlockFace.getDirection(Util.getPBlockFace(getCurrentToggleDir()));

        int blocksToMove = getBlocksToMove() > 0 ? getBlocksToMove() :
                           1 + Math.abs(vec.getY() * dimensions.getY());

        newMin.setX(minimum.getX());
        newMin.setY(minimum.getY() + blocksToMove * vec.getY());
        newMin.setZ(minimum.getZ());

        newMax.setX(maximum.getX());
        newMax.setY(maximum.getY() + blocksToMove * vec.getY());
        newMax.setZ(maximum.getZ());
        return true;
    }

    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3DiConst newMin,
                                      final @NotNull Vector3DiConst newMax, final @NotNull IPPlayer responsible,
                                      final @NotNull DoorActionType actionType)
    {

        int blocksToMove = newMin.getY() - minimum.getY();
        doorOpeningUtility.registerBlockMover(
            new VerticalMover(this, time, skipAnimation, blocksToMove, doorOpeningUtility.getMultiplier(this),
                              responsible, newMin, newMax, cause, actionType));
    }

    @Override
    public boolean equals(@Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        if (getClass() != o.getClass())
            return false;

        final @NotNull Portcullis other = (Portcullis) o;
        return blocksToMove == other.blocksToMove &&
            autoOpenTime == other.autoOpenTime &&
            autoCloseTime == other.autoCloseTime;
    }
}
