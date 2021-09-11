package nl.pim16aap2.bigdoors.doors;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.api.IPExecutor;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IPPlayerFactory;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.moveblocks.AutoCloseScheduler;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.DoorToggleResult;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@Getter
@ToString
public class DoorToggleRequest
{
    @Getter
    private final DoorRetriever.AbstractRetriever doorRetriever;
    @Getter
    private final DoorActionCause doorActionCause;
    @Getter
    private final IMessageable messageReceiver;
    @Getter
    private final @Nullable IPPlayer responsible;
    @Getter
    private final double time;
    @Getter
    private final boolean skipAnimation;
    @Getter
    private final DoorActionType doorActionType;

    private final IPLogger logger;
    private final ILocalizer localizer;
    private final DoorActivityManager doorActivityManager;
    private final AutoCloseScheduler autoCloseScheduler;
    private final IPPlayerFactory playerFactory;
    private final IBigDoorsPlatform bigDoorsPlatform;
    private final IPExecutor executor;
    private final CompletableFutureHandler handler;

    @AssistedInject
    public DoorToggleRequest(@Assisted DoorRetriever.AbstractRetriever doorRetriever,
                             @Assisted DoorActionCause doorActionCause, @Assisted IMessageable messageReceiver,
                             @Assisted @Nullable IPPlayer responsible, @Assisted double time,
                             @Assisted boolean skipAnimation, @Assisted DoorActionType doorActionType, IPLogger logger,
                             ILocalizer localizer, DoorActivityManager doorActivityManager,
                             AutoCloseScheduler autoCloseScheduler, IPPlayerFactory playerFactory,
                             IBigDoorsPlatform bigDoorsPlatform, IPExecutor executor, CompletableFutureHandler handler)
    {
        this.doorRetriever = doorRetriever;
        this.doorActionCause = doorActionCause;
        this.messageReceiver = messageReceiver;
        this.responsible = responsible;
        this.time = time;
        this.skipAnimation = skipAnimation;
        this.doorActionType = doorActionType;
        this.logger = logger;
        this.localizer = localizer;
        this.doorActivityManager = doorActivityManager;
        this.autoCloseScheduler = autoCloseScheduler;
        this.playerFactory = playerFactory;
        this.bigDoorsPlatform = bigDoorsPlatform;
        this.executor = executor;
        this.handler = handler;
    }

    /**
     * Executes the toggle request.
     *
     * @return The result of the request.
     */
    public CompletableFuture<DoorToggleResult> execute()
    {
        logger.logMessage(Level.FINE, () -> "Executing toggle request: " + this);
        return doorRetriever.getDoor().thenCompose(this::execute)
                            .exceptionally(throwable -> handler.exceptionally(throwable, DoorToggleResult.ERROR));
    }

    private CompletableFuture<DoorToggleResult> execute(Optional<AbstractDoor> doorOpt)
    {
        if (doorOpt.isEmpty())
        {
            logger.logMessage(Level.INFO, () -> "Toggle failure (no door found): " + this);
            return CompletableFuture.completedFuture(DoorToggleResult.ERROR);
        }
        final AbstractDoor door = doorOpt.get();
        final IPPlayer actualResponsible = getActualResponsible(door);

        if (bigDoorsPlatform.isMainThread())
            return CompletableFuture.completedFuture(execute(door, actualResponsible));
        return executor.supplyOnMainThread(() -> execute(door, actualResponsible));
    }

    private DoorToggleResult execute(AbstractDoor door, IPPlayer responsible)
    {
        if (!bigDoorsPlatform.isMainThread())
            throw new IllegalThreadStateException("Doors must be animated from the main thread!");
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
        DoorToggleRequest create(DoorRetriever.AbstractRetriever doorRetriever, DoorActionCause doorActionCause,
                                 IMessageable messageReceiver, @Nullable IPPlayer responsible, double time,
                                 boolean skipAnimation, DoorActionType doorActionType);
    }
}
