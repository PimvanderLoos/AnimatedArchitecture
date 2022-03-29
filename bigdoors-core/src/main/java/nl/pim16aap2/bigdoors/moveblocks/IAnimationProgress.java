package nl.pim16aap2.bigdoors.moveblocks;

import nl.pim16aap2.bigdoors.util.Cuboid;

/**
 * Keeps track of the progress of an animation.
 *
 * @author Pim
 */
@SuppressWarnings("unused")
public interface IAnimationProgress
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
     * @return The current state of the animation.
     */
    State getState();

    /**
     * Represents the various stages an animation can be in.
     */
    enum State
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
         * The animation has finished and the animated blocks are in the process of finishing up.
         */
        FINISHING,

        /**
         * The animation has completed; there are no animated blocks left.
         */
        COMPLETED
    }
}
