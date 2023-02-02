package nl.pim16aap2.bigdoors.moveblocks;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.Getter;
import nl.pim16aap2.bigdoors.api.IConfigLoader;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.audio.IAudioPlayer;
import nl.pim16aap2.bigdoors.events.structureaction.StructureActionCause;
import nl.pim16aap2.bigdoors.events.structureaction.StructureActionType;
import nl.pim16aap2.bigdoors.managers.AnimationHookManager;
import nl.pim16aap2.bigdoors.structures.StructureSnapshot;
import nl.pim16aap2.bigdoors.util.Cuboid;

import javax.inject.Named;

/**
 * Simple class that holds the data of a toggle request that is passed to the BlockMover.
 */
@Getter
public final class StructureRequestData
{
    private final StructureActivityManager structureActivityManager;
    private final IAudioPlayer audioPlayer;
    private final IPExecutor executor;
    private final AnimationHookManager animationHookManager;
    private final IConfigLoader config;
    private final int serverTickTime;
    private final StructureSnapshot structureSnapshot;
    private final StructureActionCause cause;
    private final double animationTime;
    private final boolean animationSkipped;
    private final Cuboid newCuboid;
    private final IPPlayer responsible;
    private final AnimationType animationType;
    private final StructureActionType actionType;

    @AssistedInject StructureRequestData(
        StructureActivityManager structureActivityManager,
        IAudioPlayer audioPlayer,
        IPExecutor executor,
        AnimationHookManager animationHookManager,
        IConfigLoader config,
        @Named("serverTickTime") int serverTickTime,
        @Assisted StructureSnapshot structureSnapshot,
        @Assisted StructureActionCause cause,
        @Assisted double animationTime,
        @Assisted boolean animationSkipped,
        @Assisted Cuboid newCuboid,
        @Assisted IPPlayer responsible,
        @Assisted AnimationType animationType,
        @Assisted StructureActionType actionType)
    {
        this.structureActivityManager = structureActivityManager;
        this.audioPlayer = audioPlayer;
        this.executor = executor;
        this.animationHookManager = animationHookManager;
        this.config = config;
        this.serverTickTime = serverTickTime;
        this.structureSnapshot = structureSnapshot;
        this.cause = cause;
        this.animationTime = animationTime;
        this.animationSkipped = animationSkipped;
        this.newCuboid = newCuboid;
        this.responsible = responsible;
        this.animationType = animationType;
        this.actionType = actionType;
    }

    /**
     * Factory for new {@link StructureRequestData} instances.
     */
    @AssistedFactory
    public interface IFactory
    {
        /**
         * @param structureSnapshot
         *     A snapshot of the structure, created before the toggle. The data in this snapshot is used as ground-truth
         *     for all actions of the mover.
         * @param cause
         *     The cause of the movement.
         * @param time
         *     The duration of the animation in seconds.
         * @param skipAnimation
         *     True to skip the animation and move the blocks to their new locations immediately.
         * @param newCuboid
         *     The cuboid that described the coordinates of the structure after the animation has concluded.
         * @param responsible
         *     The player responsible for the movement.
         * @param animationType
         *     The type of animation to apply.
         * @param actionType
         *     The type of movement to apply.
         * @return The new {@link StructureRequestData}.
         */
        StructureRequestData newToggleRequestData(
            StructureSnapshot structureSnapshot, StructureActionCause cause, double time,
            boolean skipAnimation, Cuboid newCuboid, IPPlayer responsible, AnimationType animationType,
            StructureActionType actionType);
    }
}
