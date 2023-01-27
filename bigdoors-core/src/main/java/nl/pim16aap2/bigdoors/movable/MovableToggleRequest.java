package nl.pim16aap2.bigdoors.movable;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionCause;
import nl.pim16aap2.bigdoors.events.movableaction.MovableActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.moveblocks.AnimationType;
import nl.pim16aap2.bigdoors.moveblocks.MovableActivityManager;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Getter
@ToString
@Flogger
public class MovableToggleRequest
{
    @Getter
    private final MovableRetriever movableRetriever;
    @Getter
    private final MovableActionCause cause;
    @Getter
    private final IMessageable messageReceiver;
    @Getter
    private final @Nullable IPPlayer responsible;
    @Getter
    private final @Nullable Double time;
    @Getter
    private final boolean skipAnimation;
    @Getter
    private final MovableActionType actionType;
    @Getter
    private final AnimationType animationType;

    private final ILocalizer localizer;
    private final MovableActivityManager movableActivityManager;
    private final IPPlayerFactory playerFactory;
    private final IPExecutor executor;

    @AssistedInject
    public MovableToggleRequest(
        @Assisted MovableRetriever movableRetriever, @Assisted MovableActionCause cause,
        @Assisted IMessageable messageReceiver, @Assisted @Nullable IPPlayer responsible,
        @Assisted @Nullable Double time, @Assisted boolean skipAnimation, @Assisted MovableActionType actionType,
        @Assisted AnimationType animationType,
        ILocalizer localizer, MovableActivityManager movableActivityManager, IPPlayerFactory playerFactory,
        IPExecutor executor)
    {
        this.movableRetriever = movableRetriever;
        this.cause = cause;
        this.messageReceiver = messageReceiver;
        this.responsible = responsible;
        this.time = time;
        this.skipAnimation = skipAnimation;
        this.actionType = actionType;
        this.animationType = animationType;
        this.localizer = localizer;
        this.movableActivityManager = movableActivityManager;
        this.playerFactory = playerFactory;
        this.executor = executor;
    }

    /**
     * Executes the toggle request.
     *
     * @return The result of the request.
     */
    public CompletableFuture<MovableToggleResult> execute()
    {
        log.atFine().log("Executing toggle request: %s", this);
        return movableRetriever.getMovable().thenApply(this::execute)
                               .exceptionally(throwable -> Util.exceptionally(throwable, MovableToggleResult.ERROR));
    }

    private MovableToggleResult execute(Optional<AbstractMovable> movableOpt)
    {
        if (movableOpt.isEmpty())
        {
            log.atInfo().log("Toggle failure (no movable found): %s", this);
            return MovableToggleResult.ERROR;
        }
        final AbstractMovable movable = movableOpt.get();
        final IPPlayer actualResponsible = getActualResponsible(movable);
        verifyValidity(actualResponsible);

        return movable.toggle(this, actualResponsible);
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
     * @param movable
     *     The movable for which to find the responsible player.
     * @return The player responsible for toggling the movable.
     */
    private IPPlayer getActualResponsible(AbstractMovable movable)
    {
        if (responsible != null)
            return responsible;
        return playerFactory.create(movable.getPrimeOwner().pPlayerData());
    }

    @AssistedFactory
    public interface IFactory
    {
        MovableToggleRequest create(
            MovableRetriever movableRetriever, MovableActionCause movableActionCause, IMessageable messageReceiver,
            @Nullable IPPlayer responsible, @Nullable Double time, boolean skipAnimation,
            MovableActionType movableActionType, AnimationType animationType);
    }
}
