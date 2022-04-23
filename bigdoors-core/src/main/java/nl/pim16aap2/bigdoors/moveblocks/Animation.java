package nl.pim16aap2.bigdoors.moveblocks;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.api.animatedblock.IAnimation;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.util.Cuboid;

import java.util.Collections;
import java.util.List;

public class Animation<T extends IAnimatedBlock> implements IAnimation<T>
{
    private final int duration;

    @Setter(AccessLevel.PACKAGE)
    private volatile Cuboid region;
    @Getter
    private final List<T> animatedBlocks;
    @Getter
    private final AbstractDoor door;
    @Setter(AccessLevel.PACKAGE)
    private volatile AnimationState state = AnimationState.PENDING;
    @Setter(AccessLevel.PACKAGE)
    private volatile int stepsExecuted = 0;

    Animation(int duration, Cuboid region, List<T> animatedBlocks, AbstractDoor door)
    {
        this.duration = duration;
        this.region = region;
        this.animatedBlocks = Collections.unmodifiableList(animatedBlocks);
        this.door = door;
    }

    @Override
    public Cuboid getRegion()
    {
        return region;
    }

    @Override
    public int getDuration()
    {
        return duration;
    }

    @Override
    public int getStepsExecuted()
    {
        return stepsExecuted;
    }

    @Override
    public AnimationState getState()
    {
        return state;
    }
}
