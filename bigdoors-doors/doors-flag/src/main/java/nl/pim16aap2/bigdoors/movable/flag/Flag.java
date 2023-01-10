package nl.pim16aap2.bigdoors.movable.flag;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.BlockMover;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Flag movable type.
 *
 * @author Pim
 * @see MovableBase
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Flag extends AbstractMovable implements IHorizontalAxisAligned, IPerpetualMover
{
    private static final MovableType MOVABLE_TYPE = MovableTypeFlag.get();

    @EqualsAndHashCode.Exclude
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
    @PersistentVariable
    protected final boolean northSouthAligned;

    public Flag(MovableBase base, boolean northSouthAligned)
    {
        super(base);
        this.lock = getLock();
        this.northSouthAligned = northSouthAligned;
    }

    private Flag(MovableBase base)
    {
        this(base, false); // Add tmp/default values
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }

    @Override
    protected double getLongestAnimationCycleDistance()
    {
        return 0.0D;
    }

    @Override
    public Cuboid getAnimationRange()
    {
        final Cuboid cuboid = getCuboid();
        final Vector3Di rotationPoint = getRotationPoint();
        final int halfHeight = (int) Math.ceil(cuboid.getDimensions().y() / 2.0F);

        final int maxDim = Math.max(cuboid.getDimensions().x(), cuboid.getDimensions().z());
        // Very, VERY rough estimate. But it's good enough for the time being.
        return new Cuboid(rotationPoint.add(-maxDim, -halfHeight, -maxDim),
                          rotationPoint.add(maxDim, halfHeight, maxDim));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not actually open in any direction, cycling the openDirection does not do anything.
     *
     * @return The current open direction.
     */
    @Override
    public RotateDirection cycleOpenDirection()
    {
        return getOpenDir();
    }

    @Override
    @Locked.Read
    protected BlockMover constructBlockMover(
        BlockMover.Context context, MovableSnapshot movableSnapshot, MovableActionCause cause, double time,
        boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible, MovableActionType actionType)
        throws Exception
    {
        return new FlagMover(
            context, this, movableSnapshot, time, config.getAnimationSpeedMultiplier(getType()), responsible,
            cause, actionType);
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
    public RotateDirection getCurrentToggleDir()
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
