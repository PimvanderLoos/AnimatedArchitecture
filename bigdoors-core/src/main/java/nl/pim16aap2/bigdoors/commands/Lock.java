package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
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

    protected Lock(ICommandSender commandSender, CommandContext context, DoorRetriever doorRetriever,
                   boolean lockedStatus)
    {
        super(commandSender, context, doorRetriever, DoorAttribute.LOCK);
        this.lockedStatus = lockedStatus;
    }

    /**
     * Runs the {@link Lock} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} responsible for changing the locked status of the door.
     * @param doorRetriever
     *     A {@link DoorRetriever} representing the {@link DoorBase} for which the locked status will be modified.
     * @param lock
     *     The new lock status.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context,
                                                 DoorRetriever doorRetriever, boolean lock)
    {
        return new Lock(commandSender, context, doorRetriever, lock).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.LOCK;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        final var event = context.getPlatform().getBigDoorsEventFactory()
                                 .createDoorPrepareLockChangeEvent(door, lockedStatus,
                                                                   getCommandSender().getPlayer().orElse(null));
        context.getPlatform().callDoorEvent(event);

        if (event.isCancelled())
        {
            logger.logMessage(Level.FINEST, "Event " + event + " was cancelled!");
            return CompletableFuture.completedFuture(true);
        }

        door.setLocked(lockedStatus);
        return door.syncData().thenApply(x -> true);
    }
}
