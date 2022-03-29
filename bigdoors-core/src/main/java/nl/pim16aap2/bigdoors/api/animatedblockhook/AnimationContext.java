package nl.pim16aap2.bigdoors.api.animatedblockhook;

import lombok.Value;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
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
     * The door that is being animated.
     */
    AbstractDoor door;

    /**
     * The current progress of the animation.
     */
    IAnimation<? extends IAnimatedBlock> animation;
}
