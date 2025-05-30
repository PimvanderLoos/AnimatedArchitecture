package nl.pim16aap2.animatedarchitecture.core.api.animatedblock;

import lombok.Value;
import nl.pim16aap2.animatedarchitecture.core.animation.Animation;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureSnapshot;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;

/**
 * Contains the context in which an animation takes place. For example, the structure that is being animated.
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
    Animation<? extends IAnimatedBlock> animation;
}
