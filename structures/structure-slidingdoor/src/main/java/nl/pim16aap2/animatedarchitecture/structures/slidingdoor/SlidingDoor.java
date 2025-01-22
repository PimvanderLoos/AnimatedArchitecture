package nl.pim16aap2.animatedarchitecture.structures.slidingdoor;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.BlockFace;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;

/**
 * Represents a Sliding Door structure type.
 */
@Flogger
@ToString
@EqualsAndHashCode
public class SlidingDoor implements IStructureComponent
{
//    @Deserialization
//    public SlidingDoor(BaseHolder base)
//    {
//        super(base, StructureTypeSlidingDoor.get());
//        this.lock = getLock();
//    }
//
//    /**
//     * Deprecated constructor for deserialization of version 1 where {@code blocksToMove} was a persistent variable.
//     */
//    @Deprecated
//    @Deserialization(version = 1)
//    public SlidingDoor(Structure.BaseHolder base, @PersistentVariable(value = "blocksToMove") int blocksToMove)
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
        return getBlocksToMove(structure);
    }

    @Override
    public Rectangle calculateAnimationRange(IStructureConst structure)
    {
        final Cuboid cuboid = structure.getCuboid();
        final Vector3Di min = cuboid.getMin();
        final Vector3Di max = cuboid.getMax();

        final int blocksToMove = getBlocksToMove(structure);
        final Cuboid cuboidRange = switch (getCurrentToggleDirection(structure))
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
    public boolean canSkipAnimation(IStructureConst structure)
    {
        return true;
    }

    @Override
    public MovementDirection getCycledOpenDirection(IStructureConst structure)
    {
        return IStructureComponent.cycleCardinalDirection(structure.getOpenDirection());
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates(IStructureConst structure)
    {
        final int blocksToMove = getBlocksToMove(structure);

        final Vector3Di vec = BlockFace.getDirection(Util.getBlockFace(getCurrentToggleDirection(structure)));
        return Optional.of(structure.getCuboid().move(blocksToMove * vec.x(), 0, blocksToMove * vec.z()));
    }

    @Override
    public IAnimationComponent constructAnimationComponent(IStructureConst structure, AnimationRequestData data)
    {
        return new SlidingDoorAnimationComponent(
            data,
            getCurrentToggleDirection(structure),
            getBlocksToMove(structure)
        );
    }
}
