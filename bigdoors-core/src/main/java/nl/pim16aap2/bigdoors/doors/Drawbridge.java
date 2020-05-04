package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeDrawbridge;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.BridgeMover;
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
 * Represents a DrawBrige doorType.
 *
 * @author Pim
 * @see HorizontalAxisAlignedBase
 */
public class Drawbridge extends HorizontalAxisAlignedBase implements IMovingDoorArchetype
{
    private static final DoorType DOOR_TYPE = DoorTypeDrawbridge.get();

    protected int autoCloseTimer;

    /**
     * Describes the current direction the door is pointing in when taking the engine as center.
     */
    protected PBlockFace currentDirection;

    /**
     * Describes if this drawbridge's vertical position points (when taking the engine Y value as center) up <b>(=
     * TRUE)</b> or down <b>(= FALSE)</b>
     */
    protected boolean modeUp = true;


    @NotNull
    public static Optional<AbstractDoorBase> constructor(final @NotNull DoorData doorData,
                                                         final @NotNull Object... args)
        throws Exception
    {
        @Nullable final PBlockFace currentDirection = PBlockFace.valueOf((int) args[1]);
        if (currentDirection == null)
            return Optional.empty();

        final boolean modeUP = ((int) args[2]) == 1;
        return Optional.of(new Drawbridge(doorData, (int) args[0], currentDirection, modeUP));
    }

    public static Object[] dataSupplier(final @NotNull AbstractDoorBase door)
        throws IllegalArgumentException
    {
        if (!(door instanceof Drawbridge))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Drawbridge from type: " + door.getDoorType().toString());

        final @NotNull Drawbridge drawbridge = (Drawbridge) door;
        return new Object[]{drawbridge.autoCloseTimer, PBlockFace.getValue(drawbridge.currentDirection),
                            drawbridge.isModeUp() ? 1 : 0};
    }

    public Drawbridge(final @NotNull DoorData doorData, final int autoCloseTimer, final PBlockFace currentDirection,
                      final boolean modeUp)
    {
        super(doorData);
        this.autoCloseTimer = autoCloseTimer;
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
    @NotNull
    @Override
    public PBlockFace calculateCurrentDirection()
    {
        if (!isOpen())
            return PBlockFace.UP;
        return PBlockFace.getOpposite(Util.getPBlockFace(getCurrentToggleDir()));
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
    public boolean equals(@Nullable Object o)
    {
        System.out.println("000013");
        if (!super.equals(o))
            return false;
        System.out.println("000014");
        if (getClass() != o.getClass())
            return false;

        final @NotNull Drawbridge other = (Drawbridge) o;


        if (currentDirection == null) System.out.println("000015");
        if (other.currentDirection == null) System.out.println("000016");


        if (autoCloseTimer != other.autoCloseTimer) System.out.println("000017");
        if (modeUp != other.modeUp) System.out.println("000018");
        if (!currentDirection.equals(other.currentDirection)) System.out.println("000019");

        boolean isSameDrawbridge =
            currentDirection.equals(other.currentDirection) && autoCloseTimer == other.autoCloseTimer &&
                modeUp == other.modeUp;
        System.out.println("isSameDrawbridge: " + isSameDrawbridge);

        return currentDirection.equals(other.currentDirection) && autoCloseTimer == other.autoCloseTimer &&
            modeUp == other.modeUp;
    }
}
