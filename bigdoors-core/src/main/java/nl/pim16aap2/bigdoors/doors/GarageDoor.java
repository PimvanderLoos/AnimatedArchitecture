package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
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

import java.util.Optional;

/**
 * Represents a Garage Door doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class GarageDoor extends HorizontalAxisAlignedBase implements IMovingDoorArchetype
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

    @NotNull
    public static Optional<AbstractDoorBase> constructor(final @NotNull DoorData doorData,
                                                         final @NotNull Object... args)
        throws Exception
    {
        @Nullable final PBlockFace currentDirection = PBlockFace.valueOf((int) args[2]);
        if (currentDirection == null)
            return Optional.empty();

        final int autoClose = (int) args[0];
        final boolean onNorthSouthAxis = ((int) args[1]) == 1;
        return Optional.of(new GarageDoor(doorData, autoClose, onNorthSouthAxis, currentDirection));
    }

    public static Object[] dataSupplier(final @NotNull AbstractDoorBase door)
        throws IllegalArgumentException
    {
        if (!(door instanceof GarageDoor))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a GarageDoor from type: " + door.getDoorType().toString());

        final @NotNull GarageDoor garageDoor = (GarageDoor) door;
        return new Object[]{garageDoor.getAutoClose(),
                            garageDoor.getOnNorthSouthAxis() ? 1 : 0,
                            PBlockFace.getValue(garageDoor.getCurrentDirection())};
    }

    public GarageDoor(final @NotNull DoorData doorData, final int autoClose, final boolean onNorthSouthAxis,
                      final @NotNull PBlockFace currentDirection)
    {
        super(doorData);
        setAutoClose(autoClose);
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
    public PBlockFace calculateCurrentDirection()
    {
        if (!isOpen())
            return PBlockFace.UP;

        int dX = engine.getX() - min.getX();
        int dZ = engine.getZ() - min.getZ();

        return PBlockFace.faceFromDir(new Vector3Di(Integer.compare(0, dX), 0, Integer.compare(0, dZ)));
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
                    getType().name() + "\""));
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
            onNorthSouthAxis == other.onNorthSouthAxis;
    }
}
