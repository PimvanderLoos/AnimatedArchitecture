package nl.pim16aap2.bigdoors.doors;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IPerpetualMoverArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeFlag;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.moveblocks.FlagMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a Flag doorType.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public class Flag extends HorizontalAxisAlignedBase implements IStationaryDoorArchetype, IPerpetualMoverArchetype
{
    private static final DoorType DOOR_TYPE = DoorTypeFlag.get();

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
    protected final PBlockFace flagDirection;

    @NotNull
    public static Optional<AbstractDoorBase> constructor(final @NotNull DoorData doorData,
                                                         final @NotNull Object... args)
        throws Exception
    {
        @Nullable final PBlockFace flagDirection = PBlockFace.valueOf((int) args[1]);
        if (flagDirection == null)
            return Optional.empty();

        final boolean onNorthSouthAxis = ((int) args[0]) == 1;
        return Optional.of(new Flag(doorData, onNorthSouthAxis, flagDirection));
    }

    public static Object[] dataSupplier(final @NotNull AbstractDoorBase door)
        throws IllegalArgumentException
    {
        if (!(door instanceof Flag))
            throw new IllegalArgumentException(
                "Trying to get the type-specific data for a Flag from type: " + door.getDoorType().toString());

        final @NotNull Flag flag = (Flag) door;
        return new Object[]{flag.getOnNorthSouthAxis() ? 1 : 0, PBlockFace.getValue(flag.getFlagDirection())};
    }

    public Flag(final @NotNull DoorData doorData, final boolean onNorthSouthAxis, final @NotNull PBlockFace hourArmSide)
    {
        super(doorData);
        this.onNorthSouthAxis = onNorthSouthAxis;
        flagDirection = hourArmSide;
    }

    @Deprecated
    protected Flag(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData,
                   final @NotNull EDoorType type)
    {
        super(pLogger, doorUID, doorData, type);
        onNorthSouthAxis = false;
        flagDirection = null;
    }

    @Deprecated
    protected Flag(final @NotNull PLogger pLogger, final long doorUID, final @NotNull DoorData doorData)
    {
        this(pLogger, doorUID, doorData, EDoorType.FLAG);
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
     * Gets the side the flag is on relative to its rotation point. See {@link #flagDirection}.
     *
     * @return The side of the rotation point (pole) that the flag is on.
     */
    @NotNull
    public PBlockFace getFlagDirection()
    {
        return flagDirection;
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
    public PBlockFace calculateCurrentDirection()
    {
        return engine.getZ() != min.getZ() ? PBlockFace.NORTH :
               engine.getX() != max.getX() ? PBlockFace.EAST :
               engine.getZ() != max.getZ() ? PBlockFace.SOUTH :
               engine.getX() != min.getX() ? PBlockFace.WEST : PBlockFace.NONE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not actually open in any direction, the open direction simply the same as {@link
     * #getCurrentDirection()}.
     */
    @Override
    public void setDefaultOpenDirection()
    {
        setOpenDir(Util.getRotateDirection(getCurrentDirection()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not actually open in any direction, cycling the openDirection does not do anything.
     *
     * @return The current open direction.
     */
    @NotNull
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void registerBlockMover(final @NotNull DoorActionCause cause, final double time,
                                      final boolean skipAnimation, final @NotNull Vector3Di newMin,
                                      final @NotNull Vector3Di newMax, final @Nullable IPPlayer initiator)
    {
        doorOpeningUtility.registerBlockMover(
            new FlagMover(60, this, doorOpeningUtility.getMultiplier(this), initiator));
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

        final @NotNull Flag other = (Flag) o;
        return flagDirection.equals(other.flagDirection) &&
            onNorthSouthAxis == other.onNorthSouthAxis;
    }
}
