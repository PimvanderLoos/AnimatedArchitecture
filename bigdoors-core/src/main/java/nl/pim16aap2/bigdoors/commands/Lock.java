package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.events.IBigDoorsEventCaller;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to change whether a movable is locked.
 *
 * @author Pim
 */
@ToString
@Flogger
public class Lock extends MovableTargetCommand
{
    private final boolean lockedStatus;
    private final IBigDoorsEventCaller bigDoorsEventCaller;
    private final IBigDoorsEventFactory bigDoorsEventFactory;

    @AssistedInject //
    Lock(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever, @Assisted boolean lockedStatus,
        IBigDoorsEventCaller bigDoorsEventCaller, IBigDoorsEventFactory bigDoorsEventFactory)
    {
        super(commandSender, localizer, textFactory, movableRetriever, MovableAttribute.LOCK);
        this.lockedStatus = lockedStatus;
        this.bigDoorsEventCaller = bigDoorsEventCaller;
        this.bigDoorsEventFactory = bigDoorsEventFactory;
    }

    @Override
    protected void handleDatabaseActionSuccess()
    {
        final String msg = lockedStatus ? "commands.lock.success.locked" : "commands.lock.success.unlocked";
        final var desc = getRetrievedMovableDescription();
        getCommandSender().sendSuccess(textFactory, localizer.getMessage(msg, desc.typeName(), desc.id()));
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.LOCK;
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractMovable movable)
    {
        final var event = bigDoorsEventFactory
            .createMovablePrepareLockChangeEvent(movable, lockedStatus, getCommandSender().getPlayer().orElse(null));

        bigDoorsEventCaller.callBigDoorsEvent(event);

        if (event.isCancelled())
        {
            log.atFinest().log("Event %s was cancelled!", event);
            return CompletableFuture.completedFuture(null);
        }

        movable.setLocked(lockedStatus);
        return movable.syncData().thenAccept(this::handleDatabaseActionResult);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Lock} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the locked status of the movable.
         * @param movableRetriever
         *     A {@link MovableRetrieverFactory} representing the {@link AbstractMovable} for which the locked status
         *     will be modified.
         * @param lock
         *     The new lock status.
         * @return See {@link BaseCommand#run()}.
         */
        Lock newLock(ICommandSender commandSender, MovableRetriever movableRetriever, boolean lock);
    }
}
