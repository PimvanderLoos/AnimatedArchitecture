package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents the command that is used to change whether a door is locked.
 *
 * @author Pim
 */
@ToString
@Flogger
public class Lock extends DoorTargetCommand
{
    private final boolean lockedStatus;
    private final IBigDoorsPlatform bigDoorsPlatform;
    private final IBigDoorsEventFactory bigDoorsEventFactory;

    @AssistedInject //
    Lock(@Assisted ICommandSender commandSender, ILocalizer localizer, @Assisted DoorRetriever doorRetriever,
         @Assisted boolean lockedStatus, IBigDoorsPlatform bigDoorsPlatform, IBigDoorsEventFactory bigDoorsEventFactory)
    {
        super(commandSender, localizer, doorRetriever, DoorAttribute.LOCK);
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
            log.at(Level.FINEST).log("Event %s was cancelled!", event);
            return CompletableFuture.completedFuture(true);
        }

        door.setLocked(lockedStatus);
        return door.syncData().thenApply(x -> true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Lock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the locked status of the door.
         * @param doorRetriever
         *     A {@link DoorRetrieverFactory} representing the {@link DoorBase} for which the locked status will be
         *     modified.
         * @param lock
         *     The new lock status.
         * @return See {@link BaseCommand#run()}.
         */
        Lock newLock(ICommandSender commandSender, DoorRetriever doorRetriever, boolean lock);
    }
}
