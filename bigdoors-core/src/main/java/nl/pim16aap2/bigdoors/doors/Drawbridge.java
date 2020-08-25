package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IHorizontalAxisAlignedDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IMovingDoorArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleableArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeDrawbridge;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BridgeMover;
import nl.pim16aap2.bigdoors.util.PLogger;
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
     * Describes if this drawbridge's vertical position points (when taking the engine Y value as center) up <b>(=
     * TRUE)</b> or down <b>(= FALSE)</b>
     *
     * @return True if this {@link Drawbridge}'s vertical stance points up.
     */
    @Getter
    protected boolean modeUp;

    public Drawbridge(final @NotNull DoorData doorData, final int autoCloseTime, final int autoOpenTime,
                      final boolean modeUp)
    {
        super(doorData);
        this.autoOpenTime = autoOpenTime;
        this.autoCloseTime = autoCloseTime;
        this.modeUp = modeUp;
    }

    public Drawbridge(final @NotNull DoorData doorData, final boolean modeUp)
    {
        this(doorData, -1, -1, modeUp);
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
    public RotateDirection getCurrentToggleDir()
    {
        return isOpen() ? RotateDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    public boolean getPotentialNewCoordinates(final @NotNull Vector3Di newMin, final @NotNull Vector3Di newMax)
    {
        final @NotNull RotateDirection rotateDirection = getCurrentToggleDir();
        final double angle;
        if (rotateDirection == RotateDirection.NORTH || rotateDirection == RotateDirection.WEST)
            angle = -Math.PI / 2;
        else if (rotateDirection == RotateDirection.SOUTH || rotateDirection == RotateDirection.EAST)
            angle = Math.PI / 2;
        else
        {
            PLogger.get().severe("Invalid open direction \"" + rotateDirection.name() + "\" for door: " + getDoorUID());
            return false;
        }

        final @NotNull IVector3DiConst newMinTmp;
        final @NotNull IVector3DiConst newMaxTmp;
        if (rotateDirection == RotateDirection.NORTH || rotateDirection == RotateDirection.SOUTH)
        {
            newMinTmp = newMin.clone().rotateAroundXAxis(getEngine(), angle);
            newMaxTmp = newMax.clone().rotateAroundXAxis(getEngine(), angle);
        }
        else
        {
            newMinTmp = newMin.clone().rotateAroundZAxis(getEngine(), angle);
            newMaxTmp = newMax.clone().rotateAroundZAxis(getEngine(), angle);
        }

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
            new BridgeMover(time, this, getCurrentToggleDir(), skipAnimation, doorOpeningUtility
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
        return autoCloseTime == other.autoCloseTime &&
            autoOpenTime == other.autoOpenTime &&
            modeUp == other.modeUp;
    }

    @Override
    public boolean isNorthSouthAligned()
    {
        final @NotNull RotateDirection openDir = getOpenDir();
        return openDir == RotateDirection.NORTH || openDir == RotateDirection.SOUTH;
    }
}
