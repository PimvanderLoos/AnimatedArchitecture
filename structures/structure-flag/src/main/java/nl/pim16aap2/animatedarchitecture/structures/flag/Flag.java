package nl.pim16aap2.animatedarchitecture.structures.flag;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;

import java.util.Optional;

/**
 * Represents a Flag structure type.
 */
@Flogger
@ToString
@EqualsAndHashCode
public class Flag implements IStructureComponent
{
//    /**
//     * Describes if the {@link Flag} is situated along the North/South axis <b>(= TRUE)</b> or along the East/West axis
//     * <b>(= FALSE)</b>.
//     * <p>
//     * To be situated along a specific axis means that the blocks move along that axis. For example, if the structure
//     * moves along the North/South <i>(= Z)</i> axis.
//     *
//     * @return True if this structure is animated along the North/South axis.
//     */
//    @Getter
//    @PersistentVariable(value = "northSouthAnimated")
//    protected final boolean northSouthAnimated;
//
//    @Deserialization
//    public Flag(BaseHolder base, @PersistentVariable(value = "northSouthAnimated") boolean northSouthAnimated)
//    {
//        super(base, StructureTypeFlag.get());
//        this.lock = getLock();
//        this.northSouthAnimated = northSouthAnimated;
//    }

    @Override
    public boolean canMovePerpetually(IStructureConst structure)
    {
        return true;
    }

    @Override
    public double calculateAnimationCycleDistance(IStructureConst structure)
    {
        return 0.0D;
    }

    @Override
    public Rectangle calculateAnimationRange(IStructureConst structure)
    {
        final Cuboid cuboid = structure.getCuboid();
        final Vector3Di rotationPoint = structure.getRequiredPropertyValue(Property.ROTATION_POINT);
        final int halfHeight = MathUtil.ceil(cuboid.getDimensions().y() / 2.0F);

        final int maxDim = Math.max(cuboid.getDimensions().x(), cuboid.getDimensions().z());
        // Very, VERY rough estimate. But it's good enough (for the time being).
        return new Cuboid(
            rotationPoint.add(-maxDim, -halfHeight, -maxDim),
            rotationPoint.add(maxDim, halfHeight, maxDim)
        ).asFlatRectangle();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Because flags do not actually open in any direction, cycling the openDirection does not do anything.
     *
     * @return The current open direction.
     */
    @Override
    public MovementDirection getCycledOpenDirection(IStructureConst structure)
    {
        return structure.getOpenDirection();
    }

    @Override
    public IAnimationComponent constructAnimationComponent(IStructureConst structure, AnimationRequestData data)
    {
        return new FlagAnimationComponent(data, isNorthSouthAnimated(structure));
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates(IStructureConst structure)
    {
        return Optional.of(structure.getCuboid());
    }
}
