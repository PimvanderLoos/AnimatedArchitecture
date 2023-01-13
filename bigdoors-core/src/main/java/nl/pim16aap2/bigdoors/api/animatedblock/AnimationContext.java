package nl.pim16aap2.bigdoors.api.animatedblock;

import lombok.Value;
import nl.pim16aap2.bigdoors.doors.DoorSnapshot;
import nl.pim16aap2.bigdoors.doortypes.DoorType;

/**
 * Contains the context in which an animation takes place. For example, the door that is being animated.
 *
 * @author Pim
 */
@Value
public class AnimationContext
{
    /**
     * The type of door being animated.
     */
    DoorType doorType;

    /**
     * A snapshot of the door being animated, created before the movement started.
     */
    DoorSnapshot doorSnapshot;

    /**
     * The current progress of the animation.
     */
    IAnimation<? extends IAnimatedBlock> animation;
}
