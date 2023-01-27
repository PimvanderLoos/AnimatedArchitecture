package nl.pim16aap2.bigdoors.movable.portcullis;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.annotations.InheritedLockField;
import nl.pim16aap2.bigdoors.annotations.PersistentVariable;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.movablearchetypes.IDiscreteMovement;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.moveblocks.MovementRequestData;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Rectangle;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;

import javax.annotation.concurrent.GuardedBy;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Portcullis movable type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Portcullis extends AbstractMovable implements IDiscreteMovement
{
    private static final MovableType MOVABLE_TYPE = MovableTypePortcullis.get();

    @EqualsAndHashCode.Exclude
    @InheritedLockField
    private final ReentrantReadWriteLock lock;

    @PersistentVariable
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    protected int blocksToMove;

    public Portcullis(AbstractMovable.MovableBaseHolder base, int blocksToMove)
    {
        super(base);
        this.lock = getLock();
        this.blocksToMove = blocksToMove;
    }

    @SuppressWarnings("unused")
    private Portcullis(AbstractMovable.MovableBaseHolder base)
    {
        this(base, -1); // Add tmp/default values
    }

    @Override
    @Locked.Read
    protected double calculateAnimationCycleDistance()
    {
        return blocksToMove;
    }

    @Override
    protected double calculateAnimationTime(double target)
    {
        return super.calculateAnimationTime(target + (isCurrentToggleDirUp() ? -0.2D : 0.2D));
    }

    @Override
    @Locked.Read
    protected Rectangle calculateAnimationRange()
    {
        final Cuboid cuboid = getCuboid();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        return new Cuboid(min.add(0, -blocksToMove, 0), max.add(0, blocksToMove, 0)).asFlatRectangle();
    }

    @Override
    public MovableType getType()
    {
        return MOVABLE_TYPE;
    }

    @Override
    @Locked.Read
    public MovementDirection getCurrentToggleDir()
    {
        return isOpen() ? getOpenDir() : MovementDirection.getOpposite(getOpenDir());
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates()
    {
        return Optional.of(getCuboid().move(0, getDirectedBlocksToMove(), 0));
    }

    /**
     * @return True if the current toggle dir goes up.
     */
    private boolean isCurrentToggleDirUp()
    {
        return getCurrentToggleDir() == MovementDirection.UP;
    }

    /**
     * @return The signed number of blocks to move (positive for up, negative for down).
     */
    private int getDirectedBlocksToMove()
    {
        return isCurrentToggleDirUp() ? getBlocksToMove() : -getBlocksToMove();
    }

    @Override
    @Locked.Read
    protected IAnimationComponent constructAnimationComponent(MovementRequestData data)
    {
        return new VerticalAnimationComponent(data, getDirectedBlocksToMove());
    }

    @Override
    @Locked.Write
    public void setBlocksToMove(int blocksToMove)
    {
        this.blocksToMove = blocksToMove;
        super.invalidateAnimationData();
    }
}
