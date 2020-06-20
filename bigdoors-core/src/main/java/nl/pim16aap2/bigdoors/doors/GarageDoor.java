package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeGarageDoor;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.GarageDoorMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Garage Door doorType.
 *
 * @author Pim
 * @see AbstractHorizontalAxisAlignedBase
 */
public class GarageDoor extends AbstractHorizontalAxisAlignedBase
    implements IMovingDoorArchetype, ITimerToggleableArchetype
{
    private static final DoorType DOOR_TYPE = DoorTypeGarageDoor.get();
    /**
     * Describes if the {@link Clock} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West
     * axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the door moves
     * along the North/South <i>(= Z)</i> axis, all animated blocks will have a different Z-coordinate depending on the
     * time of day and a X-coordinate depending on the X-coordinate they originally started at.
     */
    protected final boolean onNorthSouthAxis;

    /**
     * Gets the side the flag is on flag relative to it rotation point ("engine", i.e. the point).
     */
    @NotNull
    protected PBlockFace currentDirection;

    /**
     * See {@link ITimerToggleableArchetype#getAutoCloseTimer()}
     */
    protected int autoCloseTime;

    /**
     * See {@link ITimerToggleableArchetype#getAutoOpenTimer()}
     */
    protected int autoOpenTime;

    public GarageDoor(final @NotNull DoorData doorData, final int autoCloseTime, final int autoOpenTime,
                      final boolean onNorthSouthAxis,
                      final @NotNull PBlockFace currentDirection)
    {
        super(doorData);
        this.autoCloseTime = autoCloseTime;
        this.autoOpenTime = autoOpenTime;
        this.onNorthSouthAxis = onNorthSouthAxis;
        this.currentDirection = currentDirection;
    }

    @Deprecated
    protected GarageDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                         final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
        currentDirection = null;
        onNorthSouthAxis = false;
    }

    @Deprecated
    protected GarageDoor(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, EDoorType.GARAGEDOOR);
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
     * Checks if this {@link Clock} is on the North/South axis or not. See {@link #onNorthSouthAxis}.
     *
     * @return True if this door is animated along the North/South axis.
     */
    public boolean getOnNorthSouthAxis()
    {
        return onNorthSouthAxis;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public Vector2Di[] calculateChunkRange()
    {
        int radius = 0;

        if (!isOpen())
            radius = dimensions.getY() / 16 + 1;
        else
            radius =
                Math.max(dimensions.getX(), dimensions.getZ()) / 16 + 1;

        return new Vector2Di[]{new Vector2Di(getChunk().getX() - radius, getChunk().getY() - radius),
                               new Vector2Di(getChunk().getX() + radius, getChunk().getY() + radius)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpenable()
    {
        return !isOpen();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCloseable()
    {
        return isOpen();
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getDefaultOpenDirection()
    {
        if (onNorthSouthAxis())
            return RotateDirection.EAST;
        else
            return RotateDirection.NORTH;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        RotateDirection rotDir = getOpenDir();
        if (getCurrentDirection().equals(PBlockFace.UP))
            return rotDir;
        return RotateDirection.getOpposite(Util.getRotateDirection(getCurrentDirection()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        RotateDirection rotateDirection = getCurrentToggleDir();
        int minX = min.getX();
        int minY = min.getY();
        int minZ = min.getZ();
        int maxX = max.getX();
        int maxY = max.getY();
        int maxZ = max.getZ();
        int xLen = dimensions.getX();
        int yLen = dimensions.getY();
        int zLen = dimensions.getZ();

        Vector3Di rotateVec;
        try
        {
            rotateVec = PBlockFace.getDirection(Util.getPBlockFace(rotateDirection));
        }
        catch (Exception e)
        {
            PLogger.get().logException(new IllegalArgumentException(
                "RotateDirection \"" + rotateDirection.name() + "\" is not a valid direction for a door of type \"" +
                    getDoorType().toString() + "\""));
            return false;
        }

        if (getCurrentDirection().equals(PBlockFace.UP))
        {
            minY = maxY = max.getY() + 1;

            minX += rotateVec.getX();
            maxX += (1 + yLen) * rotateVec.getX();
            minZ += rotateVec.getZ();
            maxZ += (1 + yLen) * rotateVec.getZ();
        }
        else
        {
            maxY = maxY - 1;
            minY -= Math.abs(rotateVec.getX() * xLen);
            minY -= Math.abs(rotateVec.getZ() * zLen);
            minY -= 1;

            if (rotateDirection.equals(RotateDirection.SOUTH))
            {
                maxZ = maxZ + 1;
                minZ = maxZ;
            }
            else if (rotateDirection.equals(RotateDirection.NORTH))
            {
                maxZ = minZ - 1;
                minZ = maxZ;
            }
            if (rotateDirection.equals(RotateDirection.EAST))
            {
                maxX = maxX + 1;
                minX = maxX;
            }
            else if (rotateDirection.equals(RotateDirection.WEST))
            {
                maxX = minX - 1;
                minX = maxX;
            }
        }

        if (minX > maxX)
        {
            int tmp = minX;
            minX = maxX;
            maxX = tmp;
        }
        if (minZ > maxZ)
        {
            int tmp = minZ;
            minZ = maxZ;
            maxZ = tmp;
        }

        newMin.setX(minX);
        newMin.setY(minY);
        newMin.setZ(minZ);

        newMax.setX(maxX);
        newMax.setY(maxY);
        newMax.setZ(maxZ);

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
        // TODO: Get rid of this.
        double fixedTime = time < 0.5 ? 5 : time;

        doorOpeningUtility.registerBlockMover(
            new GarageDoorMover(this, fixedTime, doorOpeningUtility.getMultiplier(this), skipAnimation,
                                getCurrentDirection(), getCurrentToggleDir(), initiator, newMin, newMax));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final @Nullable Object o)
    {
        if (!super.equals(o))
            return false;

        if (getClass() != o.getClass())
            return false;

        final @NotNull GarageDoor other = (GarageDoor) o;
        return currentDirection.equals(other.currentDirection) &&
            onNorthSouthAxis == other.onNorthSouthAxis &&
            autoOpenTime == other.autoOpenTime &&
            autoCloseTime == other.autoCloseTime;
    }
}
