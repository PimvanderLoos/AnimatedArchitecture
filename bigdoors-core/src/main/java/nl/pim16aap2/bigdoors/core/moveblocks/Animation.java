package nl.pim16aap2.bigdoors.core.moveblocks;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimatedBlock;
import nl.pim16aap2.bigdoors.core.api.animatedblock.IAnimation;
import nl.pim16aap2.bigdoors.core.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.core.structuretypes.StructureType;
import nl.pim16aap2.bigdoors.core.util.Cuboid;

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
    private final StructureSnapshot structureSnapshot;
    @Getter
    private final StructureType structureType;
    @Setter(AccessLevel.PACKAGE)
    private volatile AnimationState state = AnimationState.PENDING;
    @Setter(AccessLevel.PACKAGE)
    private volatile int stepsExecuted = 0;
    @Getter
    private final AnimationType animationType;

    Animation(
        int duration, Cuboid region, List<T> animatedBlocks, StructureSnapshot structureSnapshot,
        StructureType structureType,
        AnimationType animationType)
    {
        this.duration = duration;
        this.region = region;
        this.animatedBlocks = Collections.unmodifiableList(animatedBlocks);
        this.structureSnapshot = structureSnapshot;
        this.structureType = structureType;
        this.animationType = animationType;
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
