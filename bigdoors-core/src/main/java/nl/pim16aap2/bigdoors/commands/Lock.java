package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents the command that is used to change whether a door is locked.
 *
 * @author Pim
 */
@ToString
public class Lock extends DoorTargetCommand
{
    private final boolean lockedStatus;
    private final IBigDoorsPlatform bigDoorsPlatform;
    private final IBigDoorsEventFactory bigDoorsEventFactory;

    @AssistedInject //
    Lock(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
         @Assisted DoorRetriever.AbstractRetriever doorRetriever, @Assisted boolean lockedStatus,
         IBigDoorsPlatform bigDoorsPlatform, CompletableFutureHandler handler,
         IBigDoorsEventFactory bigDoorsEventFactory)
    {
        super(commandSender, logger, localizer, doorRetriever, DoorAttribute.LOCK, handler);
        this.lockedStatus = lockedStatus;
        this.bigDoorsPlatform = bigDoorsPlatform;
        this.bigDoorsEventFactory = bigDoorsEventFactory;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.LOCK;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        final var event = bigDoorsEventFactory
            .createDoorPrepareLockChangeEvent(door, lockedStatus, getCommandSender().getPlayer().orElse(null));

        bigDoorsPlatform.callDoorEvent(event);

        if (event.isCancelled())
        {
            logger.logMessage(Level.FINEST, "Event " + event + " was cancelled!");
            return CompletableFuture.completedFuture(true);
        }

        door.setLocked(lockedStatus);
        return door.syncData().thenApply(x -> true);
    }

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link Lock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the locked status of the door.
         * @param doorRetriever
         *     A {@link DoorRetriever} representing the {@link DoorBase} for which the locked status will be modified.
         * @param lock
         *     The new lock status.
         * @return See {@link BaseCommand#run()}.
         */
        Lock newLock(ICommandSender commandSender, DoorRetriever.AbstractRetriever doorRetriever, boolean lock);
    }
}
