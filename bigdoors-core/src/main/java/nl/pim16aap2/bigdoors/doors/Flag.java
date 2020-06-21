package nl.pim16aap2.bigdoors.doors;

import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IPerpetualMoverArchetype;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IStationaryDoorArchetype;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.doortypes.DoorTypeFlag;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.FlagMover;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Flag doorType.
 *
 * @author Pim
 * @see AbstractDoorBase
 */
public class Flag extends AbstractDoorBase
    implements IHorizontalAxisAlignedDoorArchetype, IStationaryDoorArchetype, IPerpetualMoverArchetype
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
     *
     * @return True if this door is animated along the North/South axis.
     */
    @Getter(onMethod = @__({@Override}))
    protected final boolean northSouthAligned;

    /**
     * Gets the side the flag is on flag relative to it rotation point ("engine", i.e. the point).
     *
     * @return The side of the rotation point (pole) that the flag is on.
     */
    @Getter
    @NotNull
    protected final PBlockFace flagDirection;

    public Flag(final @NotNull DoorData doorData, final boolean northSouthAligned,
                final @NotNull PBlockFace flagDirection)
    {
        super(doorData);
        this.northSouthAligned = northSouthAligned;
        this.flagDirection = flagDirection;
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
     * <p>
     * Because flags do not actually open in any direction, the open direction simply the same as {@link
     * #getFlagDirection()} ()}.
     */
    @NotNull
    @Override
    public RotateDirection getDefaultOpenDirection()
    {
        return Util.getRotateDirection(getFlagDirection());
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
                                      final @NotNull Vector3Di newMax, final @Nullable IPPlayer initiator,
                                      final @NotNull DoorActionType actionType)
    {
        doorOpeningUtility.registerBlockMover(
            new FlagMover(60, this, doorOpeningUtility.getMultiplier(this), initiator, cause, actionType));
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
            northSouthAligned == other.northSouthAligned;
    }
}
