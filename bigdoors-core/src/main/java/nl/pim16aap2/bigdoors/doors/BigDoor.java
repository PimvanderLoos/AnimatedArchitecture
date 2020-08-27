package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BigDoorMover;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Big Door doorType.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public class BigDoor extends AbstractDoorBase implements IMovingDoorArchetype, ITimerToggleableArchetype
{
    @NotNull
    private static final DoorType DOOR_TYPE = DoorTypeBigDoor.get();

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int autoCloseTime;

    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int autoOpenTime;

    public BigDoor(final @NotNull DoorData doorData, final int autoCloseTime, final int autoOpenTime)
    {
        super(doorData);
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
    }

    public BigDoor(final @NotNull DoorData doorData)
    {
        this(doorData, -1, -1);
    }

    @NotNull
    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @NotNull
    @Override
    public Vector2Di[] calculateChunkRange()
    {
        // Yeah, radius might be too big, but it doesn't really matter.
        int radius = Math.max(dimensions.getX(), dimensions.getZ()) / 16 + 1;

        return new Vector2Di[]{new Vector2Di(getChunk().getX() - radius, getChunk().getY() - radius),
                               new Vector2Di(getChunk().getX() + radius, getChunk().getY() + radius)};
    }

    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.CLOCKWISE) ?
               RotateDirection.COUNTERCLOCKWISE : RotateDirection.CLOCKWISE;
    }

    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        final @NotNull RotateDirection rotateDirection = getCurrentToggleDir();
        final double angle = rotateDirection == RotateDirection.CLOCKWISE ? Math.PI / 2 :
                             rotateDirection == RotateDirection.COUNTERCLOCKWISE ? -Math.PI / 2 : 0.0D;
        if (angle == 0.0D)
        {
            PLogger.get().severe("Invalid open direction \"" + rotateDirection.name() + "\" for door: " + getDoorUID());
            return false;
        }

        final @NotNull IVector3DiConst newMinTmp = newMin.clone().rotateAroundYAxis(getEngine(), angle);
        final @NotNull IVector3DiConst newMaxTmp = newMax.clone().rotateAroundYAxis(getEngine(), angle);

        newMin.setX(Math.min(newMinTmp.getX(), newMaxTmp.getX()));
        newMin.setY(Math.min(newMinTmp.getY(), newMaxTmp.getY()));
        newMin.setZ(Math.min(newMinTmp.getZ(), newMaxTmp.getZ()));

        newMax.setX(Math.max(newMinTmp.getX(), newMaxTmp.getX()));
        newMax.setY(Math.max(newMinTmp.getY(), newMaxTmp.getY()));
        newMax.setZ(Math.max(newMinTmp.getZ(), newMaxTmp.getZ()));

        return true;
    }

    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull IVector3DiConst newMin,
                                      final @NotNull IVector3DiConst newMax, final @NotNull IPPlayer responsible,
                                      final @NotNull DoorActionType actionType)
    {
        doorOpeningUtility.registerBlockMover(
            new BigDoorMover(this, getCurrentToggleDir(), time, skipAnimation,
                             doorOpeningUtility.getMultiplier(this),
                             responsible, newMin, newMax, cause, actionType));
    }

    @Override
    public boolean equals(final @Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        if (!(o instanceof BigDoor))
            return false;

        final @NotNull BigDoor other = (BigDoor) o;
        return getAutoCloseTime() == other.getAutoCloseTime() &&
            getAutoOpenTime() == other.getAutoOpenTime();
    }
}
