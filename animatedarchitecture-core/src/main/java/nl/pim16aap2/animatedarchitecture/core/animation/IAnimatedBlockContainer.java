package nl.pim16aap2.animatedarchitecture.core.animation;

import nl.pim16aap2.animatedarchitecture.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a manager for animated blocks.
 * <p>
 * It is responsible for the creation and cleanup of animated blocks.
 * <p>
 * The most common example will be a manager that replaces existing blocks with animated blocks and places them in the
 * new location after the animation ends.
 */
public interface IAnimatedBlockContainer
{
    /**
     * Attempts to create and spawn the animated blocks for the given input.
     *
     * @param snapshot
     *     The snapshot of the structure to create the animated blocks for.
     * @param animationComponent
     *     The animation component to use for retrieving additional information for the animated blocks.
     * @return The result of the attempted creation of the animated blocks.
     */
    boolean createAnimatedBlocks(StructureSnapshot snapshot, IAnimationComponent animationComponent);

    /**
     * @return All the animated blocks that are part of the animation. In case an error occurred during the creation,
     * this will contain all the animated blocks that have been created up to the point that the problem occurred.
     */
    List<IAnimatedBlock> getAnimatedBlocks();

    /**
     * Getter for the animation region.
     *
     * @return The animation region.
     */
    @Nullable AnimationRegion getAnimationRegion();

    /**
     * Restores all spawned animated blocks to their original positions.
     * <p>
     * Should be used in case something failed.
     */
    void restoreBlocksOnFailure();

    /**
     * Handles the blocks when the animation is completed.
     * <p>
     * For the most common use-case, this means that the animated blocks will be killed and the blocks they represented
     * will be placed in their final positions.
     */
    void handleAnimationCompletion();
}
