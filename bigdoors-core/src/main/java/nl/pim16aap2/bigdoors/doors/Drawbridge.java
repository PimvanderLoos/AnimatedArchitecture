package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeDrawbridge;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.BridgeMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a DrawBrige doorType.
 *
 * @author Pim
 * @see AbstractHorizontalAxisAlignedBase
 */
public class Drawbridge extends AbstractHorizontalAxisAlignedBase
    implements IMovingDoorArchetype, ITimerToggleableArchetype
{
    private static final DoorType DOOR_TYPE = DoorTypeDrawbridge.get();

    /**
     * See {@link ITimerToggleableArchetype#getAutoCloseTimer()}
     */
    protected int autoCloseTime;

    /**
     * See {@link ITimerToggleableArchetype#getAutoOpenTimer()}
     */
    protected int autoOpenTime;

    /**
     * Describes the current direction the door is pointing in when taking the engine as center.
     */
    protected PBlockFace currentDirection;

    /**
     * Describes if this drawbridge's vertical position points (when taking the engine Y value as center) up <b>(=
     * TRUE)</b> or down <b>(= FALSE)</b>
     */
    protected boolean modeUp = true;

    public Drawbridge(final @NotNull DoorData doorData, final int autoCloseTime, final int autoOpenTime,
                      final PBlockFace currentDirection, final boolean modeUp)
    {
        super(doorData);
        this.autoOpenTime = autoOpenTime;
        this.autoCloseTime = autoCloseTime;
        this.currentDirection = currentDirection;
        this.modeUp = modeUp;
    }

    @Deprecated
    protected Drawbridge(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                         final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
    }

    @Deprecated
    protected Drawbridge(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, EDoorType.DRAWBRIDGE);
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
     * Checks if the vertical stance of this {@link Drawbridge} is up or down, as seen from the engine's y-value.
     *
     * @return True if this {@link Drawbridge}'s vertical stance points up.
     */
    public boolean isModeUp()
    {
        return modeUp;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Vector2Di[] calculateChunkRange()
    {
        int xLen = dimensions.getX();
        int yLen = dimensions.getY();
        int zLen = dimensions.getZ();

        int radius;
        if (dimensions.getY() != 1)
            radius = yLen / 16 + 1;
        else
            radius = Math.max(xLen, zLen) / 16 + 1;

        return new Vector2Di[]{new Vector2Di(getChunk().getX() - radius, getChunk().getY() - radius),
                               new Vector2Di(getChunk().getX() + radius, getChunk().getY() + radius)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            setOpenDir(RotateDirection.EAST);
        else
            setOpenDir(RotateDirection.NORTH);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? getOpenDir() : RotateDirection.getOpposite(getOpenDir());
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
    @Override
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        Vector3Di vec = PBlockFace.getDirection(getCurrentDirection());
        RotateDirection currentToggleDir = getCurrentToggleDir();
        if (isOpen())
        {
            if (onNorthSouthAxis())
            {
                newMax.setY(newMin.getY() + dimensions.getX());
                int newX = vec.getX() > 0 ? newMin.getX() : newMax.getX();
                newMin.setX(newX);
                newMax.setX(newX);
            }
            else
            {
                newMax.setY(newMin.getY() + dimensions.getZ());
                int newZ = vec.getZ() > 0 ? newMin.getZ() : newMax.getZ();
                newMin.setZ(newZ);
                newMax.setZ(newZ);
            }
        }
        else
        {
            if (onNorthSouthAxis()) // On Z-axis, i.e. Z doesn't change
            {
                newMax.setY(newMin.getY());
                newMin.add(currentToggleDir.equals(RotateDirection.WEST) ? -dimensions.getY() : 0, 0, 0);
                newMax.add(currentToggleDir.equals(RotateDirection.EAST) ? dimensions.getY() : 0, 0, 0);
            }
            else
            {
                newMax.setY(newMin.getY());
                newMin.add(0, 0, currentToggleDir.equals(RotateDirection.NORTH) ? -dimensions.getY() : 0);
                newMax.add(0, 0, currentToggleDir.equals(RotateDirection.SOUTH) ? dimensions.getY() : 0);
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3Di newMin,
                                      final @NotNull Vector3Di newMax, final @Nullable IPPlayer initiator)
    {
        PBlockFace upDown =
            Math.abs(min.getY() - max.getY()) > 0 ? PBlockFace.DOWN : PBlockFace.UP;

        doorOpeningUtility.registerBlockMover(
            new BridgeMover(time, this, upDown, getCurrentToggleDir(), skipAnimation, doorOpeningUtility
                .getMultiplier(this), initiator, newMin, newMax));
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        if (getClass() != o.getClass())
            return false;

        final @NotNull Drawbridge other = (Drawbridge) o;
        return currentDirection.equals(other.currentDirection) &&
            autoCloseTime == other.autoCloseTime &&
            autoOpenTime == other.autoOpenTime &&
            modeUp == other.modeUp;
    }
}
