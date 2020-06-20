package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeBigDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BigDoorMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
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
    private static final DoorType DOOR_TYPE = DoorTypeBigDoor.get();

    protected int autoCloseTime = 0;
    protected int autoOpenTime = 0;
    protected PBlockFace currentDirection;

    public BigDoor(final @NotNull DoorData doorData, final int autoCloseTime, final int autoOpenTime,
                   final @NotNull PBlockFace currentDirection)
    {
        super(doorData);
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
        this.currentDirection = currentDirection;
    }

    @Deprecated
    protected BigDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                      final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    @Deprecated
    protected BigDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, EDoorType.BIGDOOR);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Vector2Di[] calculateChunkRange()
    {
        // Yeah, radius might be too big, but it doesn't really matter.
        int radius = Math.max(dimensions.getX(), dimensions.getZ()) / 16 + 1;

        return new Vector2Di[]{new Vector2Di(getChunk().getX() - radius, getChunk().getY() - radius),
                               new Vector2Di(getChunk().getX() + radius, getChunk().getY() + radius)};
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getDefaultOpenDirection()
    {
        return RotateDirection.CLOCKWISE;
    }

    /**
     * Gets the side the {@link IDoorBase} is on relative to the engine.
     *
     * @return The side the {@link IDoorBase} is on relative to the engine
     */
    @NotNull
    public PBlockFace getCurrentDirection()
    {
        return currentDirection;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir().equals(RotateDirection.CLOCKWISE) ?
               RotateDirection.COUNTERCLOCKWISE : RotateDirection.CLOCKWISE;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        PBlockFace newDir;
        RotateDirection rotateDirection = getCurrentToggleDir();
        switch (getCurrentDirection())
        {
            case NORTH:
                newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? PBlockFace.EAST : PBlockFace.WEST;
                break;
            case EAST:
                newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? PBlockFace.SOUTH : PBlockFace.NORTH;
                break;
            case SOUTH:
                newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? PBlockFace.WEST : PBlockFace.EAST;
                break;
            case WEST:
                newDir = rotateDirection.equals(RotateDirection.CLOCKWISE) ? PBlockFace.NORTH : PBlockFace.SOUTH;
                break;
            default:
                PLogger.get()
                       .warn("Invalid currentDirection for BigDoor! \"" + getCurrentDirection().toString() + "\"");
                return false;
        }

        Vector3Di newVec = PBlockFace.getDirection(newDir);
        int xMin = Math.min(engine.getX(), engine.getX() + dimensions.getZ() * newVec.getX());
        int xMax = Math.max(engine.getX(), engine.getX() + dimensions.getZ() * newVec.getX());

        int zMin = Math.min(engine.getZ(), engine.getZ() + dimensions.getX() * newVec.getZ());
        int zMax = Math.max(engine.getZ(), engine.getZ() + dimensions.getX() * newVec.getZ());

        newMin.setX(xMin);
        newMin.setY(newMin.getY());
        newMin.setZ(zMin);

        newMax.setX(xMax);
        newMax.setY(newMax.getY());
        newMax.setZ(zMax);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3Di newMin,
                                      final @NotNull Vector3Di newMax, final @NotNull IPPlayer initiator,
                                      final @NotNull DoorActionType actionType)
    {
        doorOpeningUtility.registerBlockMover(
            new BigDoorMover(getCurrentToggleDir(), time, getCurrentDirection(), this, skipAnimation,
                             doorOpeningUtility.getMultiplier(this),
                             initiator, newMin, newMax, cause, actionType));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final @Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        final @NotNull BigDoor other = (BigDoor) o;
        return getCurrentDirection().equals(other.getCurrentDirection()) &&
            getAutoCloseTimer() == other.getAutoCloseTimer() &&
            getAutoOpenTimer() == other.getAutoOpenTimer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAutoCloseTimer(int newValue)
    {
        autoCloseTime = newValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAutoCloseTimer()
    {
        return autoCloseTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAutoOpenTimer(int newValue)
    {
        autoOpenTime = newValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAutoOpenTimer()
    {
        return autoOpenTime;
    }
}
