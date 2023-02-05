package nl.pim16aap2.bigdoors.core.api.animatedblock;

import lombok.Value;
import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;

/**
 * Contains the context in which an animation takes place. For example, the structure that is being animated.
 *
 * @author Pim
 */
@Value
public class AnimationContext
{
    /**
     * The type of structure being animated.
     */
    StructureType structureType;

    /**
     * A snapshot of the structure being animated, created before the movement started.
     */
    StructureSnapshot structureSnapshot;

    /**
     * The current progress of the animation.
     */
    IAnimation<? extends IAnimatedBlock> animation;
}
