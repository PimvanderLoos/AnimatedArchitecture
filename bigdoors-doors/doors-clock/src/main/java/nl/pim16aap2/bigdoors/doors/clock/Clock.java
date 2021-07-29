package nl.pim16aap2.bigdoors.doors.clock;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents a Clock doorType.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Clock extends AbstractDoor implements IHorizontalAxisAligned
{
    @EqualsAndHashCode.Exclude
    private static final @NotNull DoorType DOOR_TYPE = DoorTypeClock.get();

    /**
     * Describes if the {@link Clock} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West
     * axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the door moves
     * along the North/South <i>(= Z)</i> axis, all animated blocks will have a different Z-coordinate depending on the
     * time of day and a X-coordinate depending on the X-coordinate they originally started at.
     *
     * @return True if this clock is situated along the north/south axis.
     */
    @Getter
    @PersistentVariable
    protected final boolean northSouthAligned;

    /**
     * Describes on which side the hour arm is. If the clock is situated along the North/South axis see {@link
     * #northSouthAligned}, then the hour arm can either be on the {@link PBlockFace#WEST} or the {@link
     * PBlockFace#EAST} side.
     * <p>
     * This is stored as a direction rather than an integer value (for example the X/Z axis value) so that it could also
     * work for {@link Clock}s that have arms that are more than 1 block deep.
     *
     * @return The side of the hour arm relative to the minute arm.
     */
    @Getter
    @Setter
    @PersistentVariable
    protected @NotNull PBlockFace hourArmSide;

    public Clock(final @NotNull DoorBase doorData, final boolean northSouthAligned,
                 final @NotNull PBlockFace hourArmSide)
    {
        super(doorData);
        this.northSouthAligned = northSouthAligned;
        this.hourArmSide = hourArmSide;
    }

    @Override
    public @NotNull DoorType getDoorType()
    {
        return DOOR_TYPE;
    }

    @Override
    public @NotNull RotateDirection cycleOpenDirection()
    {
        return getOpenDir();
    }

    @Override
    protected @NotNull BlockMover constructBlockMover(final @NotNull DoorActionCause cause, final double time,
                                                      final boolean skipAnimation, final @NotNull Cuboid newCuboid,
                                                      final @NotNull IPPlayer responsible,
                                                      final @NotNull DoorActionType actionType)
        throws Exception
    {
        return new ClockMover<>(this, getCurrentToggleDir(), responsible, cause, actionType);
    }

    @Override
    public @NotNull Optional<Cuboid> getPotentialNewCoordinates()
    {
        return Optional.of(getCuboid());
    }

    @Override
    public boolean canSkipAnimation()
    {
        return false;
    }

    @Override
    public boolean isOpenable()
    {
        return true;
    }

    @Override
    public boolean isCloseable()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always the same as {@link #getOpenDir()}, as this archetype makes no distinction between opening and closing.
     */
    @Override
    public @NotNull RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }
}
