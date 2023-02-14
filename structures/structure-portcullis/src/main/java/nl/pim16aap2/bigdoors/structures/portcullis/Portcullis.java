package nl.pim16aap2.bigdoors.structures.portcullis;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Locked;
import nl.pim16aap2.bigdoors.core.moveblocks.IAnimationComponent;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureRequestData;
import nl.pim16aap2.bigdoors.core.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.core.structures.serialization.Deserialization;
import nl.pim16aap2.bigdoors.core.structures.serialization.PersistentVariable;
import nl.pim16aap2.bigdoors.core.structures.structurearchetypes.IDiscreteMovement;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import nl.pim16aap2.bigdoors.core.util.MovementDirection;
import nl.pim16aap2.bigdoors.core.util.Rectangle;
import nl.pim16aap2.bigdoors.core.util.vector.Vector3Di;

import javax.annotation.concurrent.GuardedBy;
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
    @EqualsAndHashCode.Exclude
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ReentrantReadWriteLock lock;

    @PersistentVariable("blocksToMove")
    @GuardedBy("lock")
    @Getter(onMethod_ = @Locked.Read)
    protected int blocksToMove;

    protected Portcullis(
        BaseHolder base,
        StructureType type,
        @PersistentVariable("blocksToMove") int blocksToMove)
    {
        super(base, type);
        this.lock = getLock();
        this.blocksToMove = blocksToMove;
    }

    @Deserialization
    public Portcullis(BaseHolder base, @PersistentVariable("blocksToMove") int blocksToMove)
    {
        this(base, StructureTypePortcullis.get(), blocksToMove);
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
    protected IAnimationComponent constructAnimationComponent(StructureRequestData data)
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