package nl.pim16aap2.bigdoors.movable.clock;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.RotateDirection;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Clock movable type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Clock extends AbstractMovable implements IHorizontalAxisAligned
{
    private static final MovableType MOVABLE_TYPE = MovableTypeClock.get();

    @EqualsAndHashCode.Exclude
    private final ReentrantReadWriteLock lock;

    /**
     * Describes if the {@link Clock} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West
     * axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the movable
     * moves along the North/South <i>(= Z)</i> axis, all animated blocks will have a different Z-coordinate depending
     * on the time of day and an X-coordinate depending on the X-coordinate they originally started at.
     *
     * @return True if this clock is situated along the north/south axis.
     */
    @Getter
    @PersistentVariable
    protected final boolean northSouthAligned;

    /**
     * Describes on which side the hour arm is. If the clock is situated along the North/South axis see
     * {@link #northSouthAligned}, then the hour arm can either be on the {@link PBlockFace#WEST} or the
     * {@link PBlockFace#EAST} side.
     * <p>
     * This is stored as a direction rather than an integer value (for example the X/Z axis value) so that it could also
     * work for {@link Clock}s that have arms that are more than 1 block deep.
     *
     * @return The side of the hour arm relative to the minute arm.
     */
    @PersistentVariable
    @Getter
    protected final PBlockFace hourArmSide;

    public Clock(MovableBase base, boolean northSouthAligned, PBlockFace hourArmSide)
    {
        super(base);
        this.lock = getLock();
        this.northSouthAligned = northSouthAligned;
        this.hourArmSide = hourArmSide;
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }

    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir();
    }

    @Override
    protected double getLongestAnimationCycleDistance()
    {
        // Not needed for this type, as it is not affected by time/speed calculations anyway.
        return 0.0D;
    }

    @Override
    public Cuboid getAnimationRange()
    {
        final Cuboid cuboid = getCuboid();

        // The clock needs to be an odd-sized square, so the radius is always half the height (rounded up).
        final int circleRadius = (int) Math.ceil(cuboid.getDimensions().y() / 2.0D);
        // The distance to the corner of the box around the circle is just pythagoras with a == b.
        final int boxRadius = (int) Math.ceil(Math.sqrt(2 * Math.pow(circleRadius, 2)));
        final int delta = boxRadius - circleRadius;

        return isNorthSouthAligned() ? cuboid.grow(0, delta, delta) : cuboid.grow(delta, delta, 0);
    }

    @Override
    @Locked.Read
    protected BlockMover constructBlockMover(MovementRequestData data)
        throws Exception
    {
        return new ClockMover<>(this, data, getCurrentToggleDir());
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates()
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
    public RotateDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }
}
