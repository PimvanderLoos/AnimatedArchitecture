package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeDrawbridge;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BridgeMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import nl.pim16aap2.bigdoors.util.vector.Vector2Di;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a DrawBrige doorType.
 *
 * @author Pim
 */
public class Drawbridge extends AbstractDoorBase
    implements IHorizontalAxisAlignedDoorArchetype, IMovingDoorArchetype, ITimerToggleableArchetype
{
    @NotNull
    private static final DoorType DOOR_TYPE = DoorTypeDrawbridge.get();

    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int autoCloseTime;

    /** {@inheritDoc} */
    @Getter(onMethod = @__({@Override}))
    @Setter(onMethod = @__({@Override}))
    protected int autoOpenTime;

    /**
     * Describes the current direction the door is pointing in when taking the engine as center.
     *
     * @return The side the {@link IDoorBase} is on relative to the engine
     */
    @Getter
    protected PBlockFace currentDirection;

    /**
     * Describes if this drawbridge's vertical position points (when taking the engine Y value as center) up <b>(=
     * TRUE)</b> or down <b>(= FALSE)</b>
     *
     * @return True if this {@link Drawbridge}'s vertical stance points up.
     */
    @Getter
    protected boolean modeUp;

    /**
     * Describes if the {@link Drawbridge} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West
     * axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the door moves
     * along the North/South <i>(= Z)</i> axis, all animated blocks will have a different Z-coordinate depending on the
     * time of day and a X-coordinate depending on the X-coordinate they originally started at.
     *
     * @return True if this door is animated along the North/South axis.
     */
    @Getter(onMethod = @__({@Override}))
    protected final boolean northSouthAligned;

    public Drawbridge(final @NotNull DoorData doorData, final int autoCloseTime, final int autoOpenTime,
                      final PBlockFace currentDirection, final boolean modeUp, final boolean northSouthAligned)
    {
        super(doorData);
        this.autoOpenTime = autoOpenTime;
        this.autoCloseTime = autoCloseTime;
        this.currentDirection = currentDirection;
        this.modeUp = modeUp;
        this.northSouthAligned = northSouthAligned;
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

    /** {@inheritDoc} */
    @NotNull
    @Override
    public RotateDirection getDefaultOpenDirection()
    {
        if (isNorthSouthAligned())
            return RotateDirection.EAST;
        else
            return RotateDirection.NORTH;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? getOpenDir() : RotateDirection.getOpposite(getOpenDir());
    }

    @Override
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        IVector3DiConst vec = PBlockFace.getDirection(getCurrentDirection());
        RotateDirection currentToggleDir = getCurrentToggleDir();
        if (isOpen())
        {
            if (isNorthSouthAligned())
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
            if (isNorthSouthAligned()) // On Z-axis, i.e. Z doesn't change
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

    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull IVector3DiConst newMin,
                                      final @NotNull IVector3DiConst newMax, final @NotNull IPPlayer responsible,
                                      final @NotNull DoorActionType actionType)
    {
        PBlockFace upDown =
            Math.abs(minimum.getY() - maximum.getY()) > 0 ? PBlockFace.DOWN : PBlockFace.UP;

        doorOpeningUtility.registerBlockMover(
            new BridgeMover(time, this, upDown, getCurrentToggleDir(), skipAnimation, doorOpeningUtility
                .getMultiplier(this), responsible, newMin, newMax, cause, actionType));
    }

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
