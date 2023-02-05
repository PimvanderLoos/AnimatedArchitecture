package nl.pim16aap2.bigdoors.core.structures;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IMessageable;
import nl.pim16aap2.bigdoors.core.api.IPExecutor;
import nl.pim16aap2.bigdoors.core.api.IPPlayer;
import nl.pim16aap2.bigdoors.core.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.core.events.structureaction.StructureActionCause;
import nl.pim16aap2.bigdoors.core.events.structureaction.StructureActionType;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.moveblocks.AnimationType;
import nl.pim16aap2.bigdoors.core.moveblocks.StructureActivityManager;
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
    private final @Nullable IPPlayer responsible;
    @Getter
    private final @Nullable Double time;
    @Getter
    private final boolean skipAnimation;
    @Getter
    private final StructureActionType actionType;
    @Getter
    private final AnimationType animationType;

    private final ILocalizer localizer;
    private final StructureActivityManager structureActivityManager;
    private final IPPlayerFactory playerFactory;
    private final IPExecutor executor;

    @AssistedInject
    public StructureToggleRequest(
        @Assisted StructureRetriever structureRetriever, @Assisted StructureActionCause cause,
        @Assisted IMessageable messageReceiver, @Assisted @Nullable IPPlayer responsible,
        @Assisted @Nullable Double time, @Assisted boolean skipAnimation, @Assisted StructureActionType actionType,
        @Assisted AnimationType animationType,
        ILocalizer localizer, StructureActivityManager structureActivityManager, IPPlayerFactory playerFactory,
        IPExecutor executor)
    {
        this.structureRetriever = structureRetriever;
        this.cause = cause;
        this.messageReceiver = messageReceiver;
        this.responsible = responsible;
        this.time = time;
        this.skipAnimation = skipAnimation;
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
        return structureRetriever.getStructure().thenApply(this::execute)
                                 .exceptionally(
                                     throwable -> Util.exceptionally(throwable, StructureToggleResult.ERROR));
    }

    private StructureToggleResult execute(Optional<AbstractStructure> structureOpt)
    {
        if (structureOpt.isEmpty())
        {
            log.atInfo().log("Toggle failure (no structure found): %s", this);
            return StructureToggleResult.ERROR;
        }
        final AbstractStructure structure = structureOpt.get();
        final IPPlayer actualResponsible = getActualResponsible(structure);
        verifyValidity(actualResponsible);

        return structure.toggle(this, actualResponsible);
    }

    private void verifyValidity(IPPlayer actualResponsible)
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
    private IPPlayer getActualResponsible(AbstractStructure structure)
    {
        if (responsible != null)
            return responsible;
        return playerFactory.create(structure.getPrimeOwner().pPlayerData());
    }

    @AssistedFactory
    public interface IFactory
    {
        StructureToggleRequest create(
            StructureRetriever structureRetriever, StructureActionCause structureActionCause,
            IMessageable messageReceiver,
            @Nullable IPPlayer responsible, @Nullable Double time, boolean skipAnimation,
            StructureActionType structureActionType, AnimationType animationType);
    }
}
