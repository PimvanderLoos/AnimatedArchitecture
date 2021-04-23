package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
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
    protected Delete(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever)
    {
        super(commandSender, doorRetriever, DoorAttribute.DELETE);
    }

    /**
     * Runs the {@link Delete} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for deleting the door.
     * @param doorRetriever A {@link DoorRetriever} representing the {@link AbstractDoorBase} which will be targeted for
     *                      deletion.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final @NonNull DoorRetriever doorRetriever)
    {
        return new Delete(commandSender, doorRetriever).run();
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.DELETE;
    }

    @Override
    protected boolean isAllowed(final @NonNull AbstractDoorBase door, final boolean bypassPermission)
    {
        return hasAccessToAttribute(door, DoorAttribute.DELETE, bypassPermission);
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door)
    {
        return BigDoors.get().getDatabaseManager()
                       .deleteDoor(door, getCommandSender().getPlayer().orElse(null))
                       .thenApply(this::handleDatabaseActionResult);
    }
}
