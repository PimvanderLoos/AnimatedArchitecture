package nl.pim16aap2.animatedarchitecture.structures.slidingdoor;

import lombok.EqualsAndHashCode;
import lombok.Locked;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.annotations.Deserialization;
import nl.pim16aap2.animatedarchitecture.core.annotations.PersistentVariable;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureWithBlocksToMove;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureWithOpenStatus;
import nl.pim16aap2.animatedarchitecture.core.util.BlockFace;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Sliding Door structure type.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SlidingDoor
    extends AbstractStructure
    implements IStructureWithBlocksToMove, IStructureWithOpenStatus
{
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    @Deserialization
    public SlidingDoor(BaseHolder base)
    {
        super(base, StructureTypeSlidingDoor.get());
        this.lock = getLock();
    }

    /**
     * Deprecated constructor for deserialization of version 1 where {@code blocksToMove} was a persistent variable.
     */
    @Deprecated
    @Deserialization(version = 1)
    public SlidingDoor(AbstractStructure.BaseHolder base, @PersistentVariable(value = "blocksToMove") int blocksToMove)
    {
        this(base);
        setBlocksToMove(blocksToMove);
    }

    @Override
    protected double calculateAnimationCycleDistance()
    {
        return getBlocksToMove();
    }

    @Override
    @Locked.Read("lock")
    protected Rectangle calculateAnimationRange()
    {
        final Cuboid cuboid = getCuboid();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        final int blocksToMove = getBlocksToMove();
        final Cuboid cuboidRange = switch (getCurrentToggleDir())
        {
            case NORTH -> new Cuboid(min.add(0, 0, -blocksToMove), max.add(0, 0, 0)); // -z
            case EAST -> new Cuboid(min.add(0, 0, 0), max.add(blocksToMove, 0, 0)); // +x
            case SOUTH -> new Cuboid(min.add(0, 0, 0), max.add(0, 0, blocksToMove)); // +z
            case WEST -> new Cuboid(min.add(-blocksToMove, 0, 0), max.add(0, 0, 0)); // -x
            default -> cuboid.grow(blocksToMove, 0, blocksToMove);
        };
        return cuboidRange.asFlatRectangle();
    }

    @Override
    public boolean canSkipAnimation()
    {
        return true;
    }

    @Override
    public MovementDirection getCycledOpenDirection()
    {
        final MovementDirection openDirection = getOpenDir();
        return openDirection.equals(MovementDirection.NORTH) ? MovementDirection.EAST :
            openDirection.equals(MovementDirection.EAST) ? MovementDirection.SOUTH :
                openDirection.equals(MovementDirection.SOUTH) ? MovementDirection.WEST :
                    MovementDirection.NORTH;
    }

    @Override
    @Locked.Read("lock")
    public MovementDirection getCurrentToggleDir()
    {
        return isOpen() ? MovementDirection.getOpposite(getOpenDir()) : getOpenDir();
    }

    @Override
    @Locked.Read("lock")
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        final Vector3Di vec = BlockFace.getDirection(Util.getBlockFace(getCurrentToggleDir()));
        return Optional.of(getCuboid().move(getBlocksToMove() * vec.x(), 0, getBlocksToMove() * vec.z()));
    }

    @Override
    @Locked.Read("lock")
    protected IAnimationComponent constructAnimationComponent(AnimationRequestData data)
    {
        return new SlidingDoorAnimationComponent(data, getCurrentToggleDir(), getBlocksToMove());
    }
}
