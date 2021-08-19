package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to delete doors.
 *
 * @author Pim
 */
@ToString
public class Delete extends DoorTargetCommand
{
    protected Delete(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        super(commandSender, doorRetriever, DoorAttribute.DELETE);
    }

    /**
     * Runs the {@link Delete} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for deleting the door.
     * @param doorRetriever A {@link DoorRetriever} representing the {@link DoorBase} which will be targeted for
     *                      deletion.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        return new Delete(commandSender, doorRetriever).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.DELETE;
    }

    @Override
    protected boolean isAllowed(AbstractDoor door, boolean bypassPermission)
    {
        return hasAccessToAttribute(door, DoorAttribute.DELETE, bypassPermission);
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractDoor door)
    {
        return BigDoors.get().getDatabaseManager()
                       .deleteDoor(door, getCommandSender().getPlayer().orElse(null))
                       .thenApply(this::handleDatabaseActionResult);
    }
}
