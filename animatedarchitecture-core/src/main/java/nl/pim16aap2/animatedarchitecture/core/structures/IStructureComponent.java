package nl.pim16aap2.animatedarchitecture.core.structures;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationRequestData;
import nl.pim16aap2.animatedarchitecture.core.animation.Animator;
import nl.pim16aap2.animatedarchitecture.core.animation.IAnimationComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.IPropertyValue;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Rectangle;

import java.util.List;
import java.util.Optional;

/**
 * Represents a component of a structure.
 * <p>
 * A structure component is used to define the behavior of a structure. This includes the type of animation, the
 * animation time, etc.
 * <p>
 * When the {@link Structure} class is constructed calls any methods from this interface, it will do so under a lock.
 * <p>
 * The methods in this interface are not meant to be called directly. Instead, use the methods in the {@link Structure}
 * class.
 */
public interface IStructureComponent
{
    /**
     * Checks if the structure can move perpetually.
     * <p>
     * This applies to structure types that have no end state, such as a flag, a clock, or a windmill.
     * <p>
     * This method defaults to {@code false}.
     *
     * @param structure
     *     The structure to check.
     * @return {@code true} if the structure can move perpetually, {@code false} otherwise.
     */
    default boolean canMovePerpetually(IStructureConst structure)
    {
        return false;
    }

    /**
     * Checks if the structure is animated in the north-south direction.
     *
     * @param structure
     *     The structure to check.
     * @return {@code true} if the structure is animated in the north-south direction, {@code false} otherwise.
     */
    default boolean isNorthSouthAnimated(IStructureConst structure)
    {
        final MovementDirection openDir = structure.getOpenDirection();
        return openDir == MovementDirection.NORTH || openDir == MovementDirection.SOUTH;
    }

    /**
     * Determines if this component can skip the animation.
     * <p>
     * See {@link Structure#canSkipAnimation()} for more information.
     * <p>
     * This method defaults to {@code true} if the structure has the {@link Property#OPEN_STATUS} property, as this
     * implies that the structure has different states and a toggle without animation is a valid operation.
     *
     * @return {@code true} if the animation can be skipped, {@code false} otherwise.
     */
    default boolean canSkipAnimation(IStructureConst structure)
    {
        return !canMovePerpetually(structure) && structure.hasProperty(Property.OPEN_STATUS);
    }

    /**
     * Calculate the animation cycle distance for this structure.
     * <p>
     * This method should not be called directly. Instead, use {@link Structure#getAnimationCycleDistance()}. See the
     * documentation for that method for more information.
     *
     * @param structure
     *     The structure to calculate the animation cycle distance for.
     * @return The distance traveled per animation cycle by the block that travels the furthest.
     */
    double calculateAnimationCycleDistance(IStructureConst structure);

    /**
     * Calculate the animation range for this structure.
     * <p>
     * This method should not be called directly. Instead, use {@link Structure#getAnimationRange()}. See the
     * documentation for that method for more information.
     *
     * @param structure
     *     The structure to calculate the animation range for.
     * @return The animation range for this structure.
     */
    Rectangle calculateAnimationRange(IStructureConst structure);

    /**
     * Creates a new {@link IAnimationComponent} for the provided structure and data for this structure component.
     *
     * @param structure
     *     The structure to calculate the animation range for.
     * @param data
     *     The data for the toggle request.
     * @return A new {@link Animator} for this type of structure.
     */
    IAnimationComponent constructAnimationComponent(IStructureConst structure, AnimationRequestData data);

    /**
     * Finds the new minimum and maximum coordinates (represented by a {@link Cuboid}) of this structure that would be
     * the result of toggling it.
     *
     * @param structure
     *     The structure to calculate the animation range for.
     * @return The {@link Cuboid} that would represent the structure if it was toggled right now.
     */
    Optional<Cuboid> getPotentialNewCoordinates(IStructureConst structure);

    /**
     * Gets the direction the structure would go given its current state.
     *
     * @param structure
     *     The structure to calculate the animation range for.
     * @return The direction the structure would go if it were to be toggled.
     */
    default MovementDirection getCurrentToggleDirection(IStructureConst structure)
    {
        final IPropertyValue<Boolean> openStatus = structure.getPropertyValue(Property.OPEN_STATUS);
        final MovementDirection openDirection = structure.getOpenDirection();

        // If the open status is both non-null and true, return the opposite of the open direction.
        // Otherwise, we assume that either this structure has no open status, or is in a closed state.
        return Boolean.TRUE.equals(openStatus.value()) ? MovementDirection.getOpposite(openDirection) : openDirection;
    }

    /**
     * Cycle the toggle direction.
     * <p>
     * See {@link Structure#getCycledOpenDirection()} for more information.
     *
     * @param structure
     *     The structure to get the cycled open direction for.
     * @return The 'next' toggle direction.
     */
    default MovementDirection getCycledOpenDirection(IStructureConst structure)
    {
        final List<MovementDirection> validOpenDirections = structure.getType().getValidOpenDirectionsList();
        final MovementDirection currentDir = structure.getOpenDirection();

        if (validOpenDirections.size() <= 1)
            return currentDir;

        final int index = Math.max(0, validOpenDirections.indexOf(currentDir));
        return validOpenDirections.get((index + 1) % validOpenDirections.size());
    }

    /**
     * Gets the animation time (in seconds) of this structure.
     * <p>
     * This basically returns max(target, {@link IStructureConst#getMinimumAnimationTime()}), logging a message in case
     * the target time is too low.
     *
     * @param target
     *     The target time.
     * @return The target time if it is bigger than the minimum time, otherwise the minimum.
     */
    default double calculateAnimationTime(IStructureConst structure, double target)
    {
        final double minimum = structure.getMinimumAnimationTime();
        if (target < minimum)
        {
            LogHolder.log.atFiner().log(
                "Target animation time of %.4f seconds is less than the minimum of %.4f seconds for structure: %s.",
                target,
                minimum,
                structure.getBasicInfo()
            );
            return minimum;
        }
        return target;
    }

    /**
     * Cycles the cardinal direction.
     * <p>
     * For example, if the input is {@link MovementDirection#NORTH}, it will return {@link MovementDirection#EAST}.
     * <p>
     * If the input is not a cardinal direction, it will return {@link MovementDirection#NORTH}.
     *
     * @param direction
     *     The direction to cycle.
     * @return The next cardinal direction.
     */
    static MovementDirection cycleCardinalDirection(MovementDirection direction)
    {
        return switch (direction)
        {
            case NORTH -> MovementDirection.EAST;
            case EAST -> MovementDirection.SOUTH;
            case SOUTH -> MovementDirection.WEST;
            // WEST -> NORTH, but we also want to reset invalid values to NORTH, just in case.
            default -> MovementDirection.NORTH;
        };
    }

    /**
     * Simple nested class to hold the logger for this interface.
     */
    @Flogger
    final class LogHolder
    {
    }
}
