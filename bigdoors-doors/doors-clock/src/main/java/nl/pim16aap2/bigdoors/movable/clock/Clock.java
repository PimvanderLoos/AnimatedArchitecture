package nl.pim16aap2.bigdoors.movable.clock;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.movable.serialization.Deserialization;
import nl.pim16aap2.bigdoors.movable.serialization.PersistentVariable;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.PBlockFace;
import nl.pim16aap2.bigdoors.util.Rectangle;

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
    @EqualsAndHashCode.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
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
    @PersistentVariable("northSouthAligned")
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
    @PersistentVariable("hourArmSide")
    @Getter
    protected final PBlockFace hourArmSide;

    @Deserialization
    public Clock(
        AbstractMovable.MovableBaseHolder base,
        @PersistentVariable("northSouthAligned") boolean northSouthAligned,
        @PersistentVariable("hourArmSide") PBlockFace hourArmSide)
    {
        super(base, MovableTypeClock.get());
        this.lock = getLock();
        this.northSouthAligned = northSouthAligned;
        this.hourArmSide = hourArmSide;
    }

    public Clock(AbstractMovable.MovableBaseHolder base)
    {
        this(base, false, PBlockFace.NONE);
    }

    @Override
    protected double calculateAnimationCycleDistance()
    {
        // Not needed for this type, as it is not affected by time/speed calculations anyway.
        return 0;
    }

    @Override
    @Locked.Read
    protected Rectangle calculateAnimationRange()
    {
        final Cuboid cuboid = getCuboid();

        // The clock needs to be an odd-sized square, so the radius is always half the height (rounded up).
        final int circleRadius = (int) Math.ceil(cuboid.getDimensions().y() / 2.0D);
        // The distance to the corner of the box around the circle is just pythagoras with a == b.
        final int boxRadius = (int) Math.ceil(Math.sqrt(2 * Math.pow(circleRadius, 2)));
        final int delta = boxRadius - circleRadius;

        return (isNorthSouthAligned() ? cuboid.grow(0, delta, delta) : cuboid.grow(delta, delta, 0)).asFlatRectangle();
    }

    @Override
    public MovementDirection cycleOpenDirection()
    {
        return getOpenDir();
    }

    @Override
    @Locked.Read
    protected IAnimationComponent constructAnimationComponent(MovementRequestData data)
    {
        return new ClockAnimationComponent(data, getCurrentToggleDir(), isNorthSouthAligned());
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
    public MovementDirection getCurrentToggleDir()
    {
        return getOpenDir();
    }
}