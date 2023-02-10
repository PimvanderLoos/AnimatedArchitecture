package nl.pim16aap2.bigdoors.structures.flag;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureRequestData;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.serialization.Deserialization;
import nl.pim16aap2.bigdoors.core.structures.serialization.PersistentVariable;
import nl.pim16aap2.bigdoors.core.structures.structurearchetypes.IHorizontalAxisAligned;
import nl.pim16aap2.bigdoors.core.structures.structurearchetypes.IPerpetualMover;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MathUtil;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Rectangle;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Flag structure type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Flag extends AbstractStructure implements IHorizontalAxisAligned, IPerpetualMover
{
    @EqualsAndHashCode.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    /**
     * Describes if the {@link Flag} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West axis
     * <b>(= FALSE)</b>.
     * <p>
     * To be situated along a specific axis means that the blocks move along that axis. For example, if the structure
     * moves along the North/South <i>(= Z)</i> axis.
     *
     * @return True if this structure is animated along the North/South axis.
     */
    @Getter
    @PersistentVariable("northSouthAligned")
    protected final boolean northSouthAligned;

    @Deserialization
    public Flag(
        BaseHolder base, @PersistentVariable("northSouthAligned") boolean northSouthAligned)
    {
        super(base, StructureTypeFlag.get());
        this.lock = getLock();
        this.northSouthAligned = northSouthAligned;
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
        final int halfHeight = MathUtil.ceil(cuboid.getDimensions().y() / 2.0F);

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
    protected IAnimationComponent constructAnimationComponent(StructureRequestData data)
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
