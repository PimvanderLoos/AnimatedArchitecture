package nl.pim16aap2.animatedarchitecture.structures.clock;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureConst;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MathUtil;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;

import java.util.Optional;

/**
 * Represents a Clock structure type.
 */
@Flogger
@ToString
@EqualsAndHashCode
public class Clock implements IStructureComponent
{
//    @Getter
//    @PersistentVariable(value = "northSouthAnimated")
//    protected final boolean northSouthAnimated;
//
//    /**
//     * Describes on which side the hour arm is. If the clock is situated along the North/South axis see
//     * {@link #northSouthAnimated}, then the hour arm can either be on the {@link BlockFace#WEST} or the
//     * {@link BlockFace#EAST} side.
//     * <p>
//     * This is stored as a direction rather than an integer value (for example the X/Z axis value) so that it could also
//     * work for {@link Clock}s that have arms that are more than 1 block deep.
//     *
//     * @return The side of the hour arm relative to the minute arm.
//     */
//    @PersistentVariable(value = "hourArmSide")
//    @Getter
//    protected final BlockFace hourArmSide;
//
//    @Deserialization
//    public Clock(
//        BaseHolder base,
//        @PersistentVariable(value = "northSouthAnimated") boolean northSouthAnimated,
//        @PersistentVariable(value = "hourArmSide") BlockFace hourArmSide)
//    {
//        super(base, StructureTypeClock.get());
//        this.lock = getLock();
//        this.northSouthAnimated = northSouthAnimated;
//        this.hourArmSide = hourArmSide;
//    }
//
//    public Clock(BaseHolder base)
//    {
//        this(base, false, BlockFace.NONE);
//    }

    @Override
    public boolean canMovePerpetually(IStructureConst structure)
    {
        return true;
    }

    @Override
    public double calculateAnimationCycleDistance(IStructureConst structure)
    {
        // Not needed for this type, as this type has no real cycle.
        // Its movement is based on the in-game time.
        return 0;
    }

    @Override
    public Rectangle calculateAnimationRange(IStructureConst structure)
    {
        final Cuboid cuboid = structure.getCuboid();

        // The clock needs to be an odd-sized square, so the radius is always half the height (rounded up).
        final int circleRadius = MathUtil.ceil(cuboid.getDimensions().y() / 2.0D);
        // The distance to the corner of the box around the circle is just pythagoras with a == b.
        final int boxRadius = MathUtil.ceil(Math.sqrt(2 * Math.pow(circleRadius, 2)));
        final int delta = boxRadius - circleRadius;

        return (isNorthSouthAnimated(structure) ?
            cuboid.grow(0, delta, delta) :
            cuboid.grow(delta, delta, 0)
        ).asFlatRectangle();
    }

//    @Override
//    public double calculateAnimationTime(double target)
//    {
//        return 8;
//    }

    @Override
    public MovementDirection getCycledOpenDirection(IStructureConst structure)
    {
        return structure.getOpenDirection();
    }

    @Override
    public IAnimationComponent constructAnimationComponent(IStructureConst structure, AnimationRequestData data)
    {
        return new ClockAnimationComponent(
            data,
            getCurrentToggleDirection(structure),
            isNorthSouthAnimated(structure)
        );
    }

    @Override
    public Optional<Cuboid> getPotentialNewCoordinates(IStructureConst structure)
    {
        return Optional.of(structure.getCuboid());
    }
}
