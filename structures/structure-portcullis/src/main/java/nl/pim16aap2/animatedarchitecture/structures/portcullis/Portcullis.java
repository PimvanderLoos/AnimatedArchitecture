package nl.pim16aap2.animatedarchitecture.structures.portcullis;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Locked;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.annotations.Deserialization;
import nl.pim16aap2.animatedarchitecture.core.annotations.PersistentVariable;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.structurearchetypes.IDiscreteMovement;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a Portcullis structure type.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Portcullis extends AbstractStructure implements IDiscreteMovement
{
    @EqualsAndHashCode.Exclude @ToString.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    @PersistentVariable(value = "blocksToMove")
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read("lock"))
    protected int blocksToMove;

    protected Portcullis(
        BaseHolder base,
        StructureType type,
        @PersistentVariable(value = "blocksToMove") int blocksToMove)
    {
        super(base, type);
        this.lock = getLock();
        this.blocksToMove = blocksToMove;
    }

    @Deserialization
    public Portcullis(BaseHolder base, @PersistentVariable(value = "blocksToMove") int blocksToMove)
    {
        this(base, StructureTypePortcullis.get(), blocksToMove);
    }

    @Override
    @Locked.Read("lock")
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
    @Locked.Read("lock")
    protected Rectangle calculateAnimationRange()
    {
        final Cuboid cuboid = getCuboid();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        return new Cuboid(min.add(0, -blocksToMove, 0), max.add(0, blocksToMove, 0)).asFlatRectangle();
    }

    @Override
    @Locked.Read("lock")
    public MovementDirection getCurrentToggleDir()
    {
        return isOpen() ? MovementDirection.getOpposite(getOpenDir()) : getOpenDir();
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
    @Locked.Read("lock")
    protected IAnimationComponent constructAnimationComponent(AnimationRequestData data)
    {
        return new VerticalAnimationComponent(data, getDirectedBlocksToMove());
    }

    @Override
    @Locked.Write("lock")
    public void setBlocksToMove(int blocksToMove)
    {
        this.blocksToMove = blocksToMove;
        super.invalidateAnimationData();
    }
}
