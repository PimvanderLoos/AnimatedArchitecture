package nl.pim16aap2.animatedarchitecture.core.animation;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;

import java.util.Collections;
import java.util.List;

/**
 * Represents an animation.
 *
 * @param <T>
 *     The type of the animated block.
 */
public class Animation<T extends IAnimatedBlock>
{
    /**
     * The number of animation steps that this animation will execute. This excludes any steps taken during the cleanup
     * phase.
     */
    @Getter
    private final int duration;

    /**
     * True if the animation is 'perpetual'.
     * <p>
     * Perpetual animations will continue until aborted externally. This may happen when the area they are animating in
     * is unloaded by the server, when the server/plugin restarts, or when a user stops/deletes the structure.
     */
    @Getter
    private final boolean perpetual;
    /**
     * The region that this animation currently occupies.
     * <p>
     * It should be noted that this is only a rather inaccurate approximation, as it is a snapshot of the region of a
     * moving object. Additionally, there are further inaccuracies introduced by rounding and region buffering.
     */
    @Setter(AccessLevel.PACKAGE)
    @Getter
    private volatile Cuboid region;

    /**
     * The list of animated blocks that are used in this animation.
     */
    @Getter
    private final List<T> animatedBlocks;

    /**
     * A snapshot of the structure being animated, created before the movement started.
     */
    @Getter
    private final StructureSnapshot structureSnapshot;

    /**
     * The type of the structure being toggled.
     */
    @Getter
    private final StructureType structureType;

    /**
     * The current state of the animation.
     */
    @Setter(AccessLevel.PACKAGE)
    @Getter
    private volatile AnimationState state = AnimationState.PENDING;

    /**
     * The number of animation steps that have been executed so far during this animation. This excludes any steps taken
     * during the cleanup phase.
     */
    @Setter(AccessLevel.PACKAGE)
    @Getter
    private volatile int stepsExecuted = 0;

    /**
     * The type of the animation.
     */
    @Getter
    private final AnimationType animationType;

    /**
     * The player responsible for the animation.
     * <p>
     * This player may be the player who requested the animation, or it may be the prime owner of the structure if the
     * animation request was not made by a player.
     * <p>
     * This player may not be online.
     */
    @Getter
    private final IPlayer responsible;

    Animation(
        int duration,
        boolean perpetual,
        Cuboid region,
        List<T> animatedBlocks,
        StructureSnapshot structureSnapshot,
        StructureType structureType,
        AnimationType animationType,
        IPlayer responsible)
    {
        this.duration = duration;
        this.perpetual = perpetual;
        this.region = region;
        this.animatedBlocks = Collections.unmodifiableList(animatedBlocks);
        this.structureSnapshot = structureSnapshot;
        this.structureType = structureType;
        this.animationType = animationType;
        this.responsible = responsible;
    }

    /**
     * Returns the number of animation steps remaining in this animation.
     * <p>
     * This only includes the steps to be executed by the animation and not any steps required for the cleanup phase.
     * <p>
     * When {@link #isPerpetual()} is true, this method always returns {@link #getDuration()}.
     *
     * @return The number of animation steps remaining in this animation.
     */
    public int getRemainingSteps()
    {
        return isPerpetual() ? getDuration() : (getDuration() - stepsExecuted);
    }

    /**
     * Represents the various stages an animation can be in.
     */
    public enum AnimationState
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
