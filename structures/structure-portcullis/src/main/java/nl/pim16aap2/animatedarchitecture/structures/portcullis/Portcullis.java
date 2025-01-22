package nl.pim16aap2.animatedarchitecture.structures.portcullis;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;

/**
 * Represents a Portcullis structure type.
 */
@Flogger
@ToString
@EqualsAndHashCode
public class Portcullis implements IStructureComponent
{
//    protected Portcullis(
//        BaseHolder base,
//        StructureType type)
//    {
//        super(base, type);
//        this.lock = getLock();
//    }
//
//    @Deserialization
//    public Portcullis(BaseHolder base)
//    {
//        this(base, StructureTypePortcullis.get());
//    }
//
//    /**
//     * Deprecated constructor for deserialization of version 1 where {@code blocksToMove} was a persistent variable.
//     */
//    @Deprecated
//    @Deserialization(version = 1)
//    public Portcullis(Structure.BaseHolder base, @PersistentVariable(value = "blocksToMove") int blocksToMove)
//    {
//        this(base);
//        setBlocksToMove(blocksToMove);
//    }

    private int getBlocksToMove(IStructureConst structure)
    {
        return structure.getRequiredPropertyValue(Property.BLOCKS_TO_MOVE);
    }

    @Override
    public double calculateAnimationCycleDistance(IStructureConst structure)
    {
        return structure.getRequiredPropertyValue(Property.BLOCKS_TO_MOVE);
    }

    @Override
    public double calculateAnimationTime(IStructureConst structure, double target)
    {
        return IStructureComponent.super.calculateAnimationTime(
            structure,
            target + (isCurrentToggleDirUp(structure) ? -0.2D : 0.2D)
        );
    }

    @Override
    public Rectangle calculateAnimationRange(IStructureConst structure)
    {
        final Cuboid cuboid = structure.getCuboid();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        final int blocksToMove = getBlocksToMove(structure);
        return new Cuboid(min.add(0, -blocksToMove, 0), max.add(0, blocksToMove, 0)).asFlatRectangle();
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates(IStructureConst structure)
    {
        return Optional.of(structure.getCuboid().move(0, getDirectedBlocksToMove(structure), 0));
    }

    /**
     * @return True if the current toggle dir goes up.
     */
    private boolean isCurrentToggleDirUp(IStructureConst structure)
    {
        return getCurrentToggleDirection(structure) == MovementDirection.UP;
    }

    /**
     * @return The signed number of blocks to move (positive for up, negative for down).
     */
    private int getDirectedBlocksToMove(IStructureConst structure)
    {
        return isCurrentToggleDirUp(structure) ? getBlocksToMove(structure) : -getBlocksToMove(structure);
    }

    @Override
    public IAnimationComponent constructAnimationComponent(IStructureConst structure, AnimationRequestData data)
    {
        return new VerticalAnimationComponent(
            data,
            getDirectedBlocksToMove(structure)
        );
    }
}
