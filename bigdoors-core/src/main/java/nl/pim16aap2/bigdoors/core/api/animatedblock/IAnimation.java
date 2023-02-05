package nl.pim16aap2.bigdoors.core.api.animatedblock;

import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.util.Cuboid;

import java.util.List;

/**
 * Keeps track of the progress of an animation.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public interface IAnimation<T extends IAnimatedBlock>
{
    /**
     * @return The region that this animation currently occupies.
     * <p>
     * It should be noted that this is only a rather inaccurate approximation, as it is a snapshot of the region of a
     * moving object. Additionally, there are further inaccuracies introduced by rounding and region buffering.
     */
    Cuboid getRegion();

    /**
     * @return The number of animation steps that this animation will execute. This excludes any steps taken during the
     * cleanup phase.
     */
    int getDuration();

    /**
     * @return The number of animation steps that have been executed so far during this animation. This excludes any
     * steps taken during the cleanup phase.
     */
    int getStepsExecuted();

    /**
     * @return The number of animation steps remaining in this animation. This only includes the steps to be executed by
     * the animation and not any steps required for the cleanup phase.
     */
    default int getRemainingSteps()
    {
        return getDuration() - getStepsExecuted();
    }

    /**
     * @return The list of animated blocks that are used in this animation.
     */
    List<T> getAnimatedBlocks();

    /**
     * @return A snapshot of the structure being animated, created before the movement started.
     */
    StructureSnapshot getStructureSnapshot();

    /**
     * @return The type of the structure being toggled.
     */
    StructureType getStructureType();

    /**
     * @return The current state of the animation.
     */
    AnimationState getState();

    /**
     * Represents the various stages an animation can be in.
     */
    enum AnimationState
    {
        /**
         * The animation hasn't started yet.
         */
        PENDING,

        /**
         * The animation is currently ongoing.
         */
        ACTIVE,

        /**
         * The animation is in the process of finishing up. This means that the animated blocks are being moved to their
         * final positions.
         */
        FINISHING,

        /**
         * The animation has finished and the animated blocks are about to be placed down as normal blocks.
         */
        STOPPING,

        /**
         * The animation has completed; there are no animated blocks left.
         */
        COMPLETED,

        /**
         * The animation was skipped. This can happen either because it was requested to skip the animation or because
         * there are no blocks to animate.
         * <p>
         * Note that this only applies to the animation itself; the structure itself is still toggled and all blocks (if
         * applicable) are moved to the next location.
         */
        SKIPPED
    }
}
