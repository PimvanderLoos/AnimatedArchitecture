package nl.pim16aap2.bigdoors.api.animatedblock;

import lombok.Value;
import nl.pim16aap2.bigdoors.movable.MovableSnapshot;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;

/**
 * Contains the context in which an animation takes place. For example, the movable that is being animated.
 *
 * @author Pim
 */
@Value
public class AnimationContext
{
    /**
     * The type of movable being animated.
     */
    MovableType movableType;

    /**
     * A snapshot of the movable being animated, created before the movement started.
     */
    MovableSnapshot movableSnapshot;

    /**
     * The current progress of the animation.
     */
    IAnimation<? extends IAnimatedBlock> animation;
}
