package nl.pim16aap2.bigdoors.doors;

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
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.moveblocks.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Getter
@ToString
@Flogger
public class DoorToggleRequest
{
    @Getter
    private final DoorRetriever doorRetriever;
    @Getter
    private final DoorActionCause doorActionCause;
    @Getter
    private final IMessageable messageReceiver;
    @Getter
    private final @Nullable IPPlayer responsible;
    @Getter
    private final @Nullable Double time;
    @Getter
    private final boolean skipAnimation;
    @Getter
    private final DoorActionType doorActionType;

    private final ILocalizer localizer;
    private final DoorActivityManager doorActivityManager;
    private final AutoCloseScheduler autoCloseScheduler;
    private final IPPlayerFactory playerFactory;
    private final IPExecutor executor;

    @AssistedInject
    public DoorToggleRequest(
        @Assisted DoorRetriever doorRetriever, @Assisted DoorActionCause doorActionCause,
        @Assisted IMessageable messageReceiver, @Assisted @Nullable IPPlayer responsible,
        @Assisted @Nullable Double time, @Assisted boolean skipAnimation, @Assisted DoorActionType doorActionType,
        ILocalizer localizer, DoorActivityManager doorActivityManager, AutoCloseScheduler autoCloseScheduler,
        IPPlayerFactory playerFactory, IPExecutor executor)
    {
        this.doorRetriever = doorRetriever;
        this.doorActionCause = doorActionCause;
        this.messageReceiver = messageReceiver;
        this.responsible = responsible;
        this.time = time;
        this.skipAnimation = skipAnimation;
        this.doorActionType = doorActionType;
        this.localizer = localizer;
        this.doorActivityManager = doorActivityManager;
        this.autoCloseScheduler = autoCloseScheduler;
        this.playerFactory = playerFactory;
        this.executor = executor;
    }

    /**
     * Executes the toggle request.
     *
     * @return The result of the request.
     */
    public CompletableFuture<DoorToggleResult> execute()
    {
        log.at(Level.FINE).log("Executing toggle request: %s", this);
        return doorRetriever.getDoor().thenApply(this::execute)
                            .exceptionally(throwable -> Util.exceptionally(throwable, DoorToggleResult.ERROR));
    }

    private DoorToggleResult execute(Optional<AbstractDoor> doorOpt)
    {
        if (doorOpt.isEmpty())
        {
            log.at(Level.INFO).log("Toggle failure (no door found): %s", this);
            return DoorToggleResult.ERROR;
        }
        final AbstractDoor door = doorOpt.get();
        final IPPlayer actualResponsible = getActualResponsible(door);
        return execute(door, actualResponsible);
    }

    private DoorToggleResult execute(AbstractDoor door, IPPlayer responsible)
    {
        return door.toggle(doorActionCause, messageReceiver, responsible, time, skipAnimation, doorActionType);
    }

    /**
     * Gets the player responsible for this toggle. When {@link #responsible} is provided, this will be the responsible
     * player.
     * <p>
     * If {@link #responsible} is null, the prime owner will be used as responsible player.
     *
     * @param door
     *     The door for which to find the responsible player.
     * @return The player responsible for toggling the door.
     */
    private IPPlayer getActualResponsible(AbstractDoor door)
    {
        if (responsible != null)
            return responsible;
        return playerFactory.create(door.getPrimeOwner().pPlayerData());
    }

    @AssistedFactory
    public interface IFactory
    {
        DoorToggleRequest create(
            DoorRetriever doorRetriever, DoorActionCause doorActionCause, IMessageable messageReceiver,
            @Nullable IPPlayer responsible, @Nullable Double time, boolean skipAnimation,
            DoorActionType doorActionType);
    }
}
