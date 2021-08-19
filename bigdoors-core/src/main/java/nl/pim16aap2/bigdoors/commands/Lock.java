package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents the command that is used to change whether or not a door is locked.
 *
 * @author Pim
 */
@ToString
public class Lock extends DoorTargetCommand
{
    private final boolean lockedStatus;

    protected Lock(ICommandSender commandSender, DoorRetriever doorRetriever, boolean lockedStatus)
    {
        super(commandSender, doorRetriever, DoorAttribute.LOCK);
        this.lockedStatus = lockedStatus;
    }

    /**
     * Runs the {@link Lock} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for changing the locked status of the door.
     * @param doorRetriever A {@link DoorRetriever} representing the {@link DoorBase} for which the locked status will
     *                      be modified.
     * @param lock          The new lock status.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, DoorRetriever doorRetriever,
                                                 boolean lock)
    {
        return new Lock(commandSender, doorRetriever, lock).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.LOCK;
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        val event = BigDoors.get().getPlatform().getBigDoorsEventFactory()
                            .createDoorPrepareLockChangeEvent(door, lockedStatus,
                                                              getCommandSender().getPlayer().orElse(null));
        BigDoors.get().getPlatform().callDoorEvent(event);

        if (event.isCancelled())
        {
            BigDoors.get().getPLogger().logMessage(Level.FINEST, "Event " + event + " was cancelled!");
            return CompletableFuture.completedFuture(true);
        }

        door.setLocked(lockedStatus);
        return door.syncData().thenApply(x -> true);
    }
}
