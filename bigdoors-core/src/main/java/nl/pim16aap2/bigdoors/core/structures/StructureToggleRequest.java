package nl.pim16aap2.bigdoors.core.structures;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.core.api.IExecutor;
import nl.pim16aap2.bigdoors.core.api.IMessageable;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.IPlayerFactory;
import nl.pim16aap2.bigdoors.core.events.StructureActionCause;
import nl.pim16aap2.bigdoors.core.events.StructureActionType;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.moveblocks.AnimationType;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureActivityManager;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureRequestData;
import nl.pim16aap2.bigdoors.core.util.Util;
import nl.pim16aap2.bigdoors.core.util.structureretriever.StructureRetriever;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter
@ToString
@Flogger
public class StructureToggleRequest
{
    @Getter
    private final StructureRetriever structureRetriever;
    @Getter
    private final StructureActionCause cause;
    @Getter
    private final IMessageable messageReceiver;
    @Getter
    private final @Nullable IPlayer responsible;
    @Getter
    private final @Nullable Double time;
    @Getter
    private final boolean skipAnimation;
    @Getter
    private final boolean preventPerpetualMovement;
    @Getter
    private final StructureActionType actionType;
    @Getter
    private final AnimationType animationType;

    private final ILocalizer localizer;
    private final StructureActivityManager structureActivityManager;
    private final IPlayerFactory playerFactory;
    private final IExecutor executor;

    @AssistedInject
    public StructureToggleRequest(
        @Assisted StructureRetriever structureRetriever,
        @Assisted StructureActionCause cause,
        @Assisted IMessageable messageReceiver,
        @Assisted @Nullable IPlayer responsible,
        @Assisted @Nullable Double time,
        @Assisted boolean skipAnimation,
        @Assisted boolean preventPerpetualMovement,
        @Assisted StructureActionType actionType,
        @Assisted AnimationType animationType,
        ILocalizer localizer,
        StructureActivityManager structureActivityManager,
        IPlayerFactory playerFactory,
        IExecutor executor)
    {
        this.structureRetriever = structureRetriever;
        this.cause = cause;
        this.messageReceiver = messageReceiver;
        this.responsible = responsible;
        this.time = time;
        this.skipAnimation = skipAnimation;
        this.preventPerpetualMovement = preventPerpetualMovement;
        this.actionType = actionType;
        this.animationType = animationType;
        this.localizer = localizer;
        this.structureActivityManager = structureActivityManager;
        this.playerFactory = playerFactory;
        this.executor = executor;
    }

    /**
     * Executes the toggle request.
     *
     * @return The result of the request.
     */
    public CompletableFuture<StructureToggleResult> execute()
    {
        log.atFine().log("Executing toggle request: %s", this);
        return structureRetriever
            .getStructure().thenApply(this::execute)
            .exceptionally(throwable -> Util.exceptionally(throwable, StructureToggleResult.ERROR));
    }

    private StructureToggleResult execute(Optional<AbstractStructure> structureOpt)
    {
        if (structureOpt.isEmpty())
        {
            log.atInfo().log("Toggle failure (no structure found): %s", this);
            return StructureToggleResult.ERROR;
        }
        final AbstractStructure structure = structureOpt.get();
        final IPlayer actualResponsible = getActualResponsible(structure);
        verifyValidity(actualResponsible);

        return structure.toggle(this, actualResponsible);
    }

    private void verifyValidity(IPlayer actualResponsible)
    {
        if (this.animationType == AnimationType.PREVIEW && !actualResponsible.isOnline())
            throw new IllegalStateException("Trying to show preview to offline player: " + actualResponsible);
    }

    /**
     * Gets the player responsible for this toggle. When {@link #responsible} is provided, this will be the responsible
     * player.
     * <p>
     * If {@link #responsible} is null, the prime owner will be used as responsible player.
     *
     * @param structure
     *     The structure for which to find the responsible player.
     * @return The player responsible for toggling the structure.
     */
    private IPlayer getActualResponsible(AbstractStructure structure)
    {
        if (responsible != null)
            return responsible;
        return playerFactory.create(structure.getPrimeOwner().playerData());
    }

    @AssistedFactory
    public interface IFactory
    {
        /**
         * @param structureRetriever
         *     A retriever for the structure for which this toggle request will be created.
         *     <p>
         *     Note that the retriever may only specify a single structure. See
         *     {@link StructureRetriever#getStructure()}.
         * @param cause
         *     The cause of the movement.
         * @param messageReceiver
         *     The receiver for all messages related to the toggle. When no player is available, this should usually be
         *     the server (See {@link IBigDoorsPlatform#getServer()}) or {@link IMessageable.BlackHoleMessageable}.
         * @param time
         *     The duration of the animation in seconds. May be null to use the default time.
         * @param skipAnimation
         *     True to skip the animation and move the blocks to their new locations immediately.
         * @param preventPerpetualMovement
         *     True to prevent perpetual movement. When perpetual movement is requested but denied via this setting, the
         *     animation will still be time-limited.
         * @param responsible
         *     The player responsible for the movement.
         * @param animationType
         *     The type of animation to apply.
         * @param actionType
         *     The type of movement to apply.
         * @return The new {@link StructureRequestData}.
         */
        StructureToggleRequest create(
            StructureRetriever structureRetriever, StructureActionCause cause,
            IMessageable messageReceiver, @Nullable IPlayer responsible, @Nullable Double time, boolean skipAnimation,
            boolean preventPerpetualMovement, StructureActionType actionType, AnimationType animationType);
    }
}
