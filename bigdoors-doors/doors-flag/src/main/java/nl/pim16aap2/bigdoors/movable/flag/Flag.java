package nl.pim16aap2.bigdoors.movable.flag;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.movable.serialization.DeserializationConstructor;
import nl.pim16aap2.bigdoors.movable.serialization.PersistentVariable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Rectangle;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Flag movable type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Flag extends AbstractMovable implements IHorizontalAxisAligned, IPerpetualMover
{
    private static final MovableType MOVABLE_TYPE = MovableTypeFlag.get();

    @EqualsAndHashCode.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    /**
     * Describes if the {@link Flag} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the movable
     * moves along the North/South <i>(= Z)</i> axis.
     *
     * @return True if this movable is animated along the North/South axis.
     */
    @Getter
    @PersistentVariable("northSouthAligned")
    protected final boolean northSouthAligned;

    @DeserializationConstructor
    public Flag(
        AbstractMovable.MovableBaseHolder base, @PersistentVariable("northSouthAligned") boolean northSouthAligned)
    {
        super(base);
        this.lock = getLock();
        this.northSouthAligned = northSouthAligned;
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }

    @Override
    protected double calculateAnimationCycleDistance()
    {
        return 0.0D;
    }

    @Override
    @Locked.Read
    protected Rectangle calculateAnimationRange()
    {
        final Cuboid cuboid = getCuboid();
        final Vector3Di rotationPoint = getRotationPoint();
        final int halfHeight = (int) Math.ceil(cuboid.getDimensions().y() / 2.0F);

        final int maxDim = Math.max(cuboid.getDimensions().x(), cuboid.getDimensions().z());
        // Very, VERY rough estimate. But it's good enough for the time being.
        return new Cuboid(rotationPoint.add(-maxDim, -halfHeight, -maxDim),
                          rotationPoint.add(maxDim, halfHeight, maxDim)).asFlatRectangle();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not actually open in any direction, cycling the openDirection does not do anything.
     *
     * @return The current open direction.
     */
    @Override
    public MovementDirection cycleOpenDirection()
    {
        return getOpenDir();
    }

    @Override
    @Locked.Read
    protected IAnimationComponent constructAnimationComponent(MovementRequestData data)
    {
        return new FlagAnimationComponent(data, isNorthSouthAligned());
    }

    @Override
    public boolean canSkipAnimation()
    {
        return false;
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        return Optional.of(getCuboid());
    }

    @Override
    public MovementDirection getCurrentToggleDir()
    {
        return getOpenDir();
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
}
