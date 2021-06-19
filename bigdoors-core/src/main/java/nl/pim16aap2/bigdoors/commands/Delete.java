package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to delete doors.
 *
 * @author Pim
 */
@ToString
public class Delete extends DoorTargetCommand
{
    protected Delete(final @NotNull ICommandSender commandSender, final @NotNull DoorRetriever doorRetriever)
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
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull DoorRetriever doorRetriever)
    {
        return new Delete(commandSender, doorRetriever).run();
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return CommandDefinition.DELETE;
    }

    @Override
    protected boolean isAllowed(final @NotNull AbstractDoorBase door, final boolean bypassPermission)
    {
        return hasAccessToAttribute(door, DoorAttribute.DELETE, bypassPermission);
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> performAction(final @NotNull AbstractDoorBase door)
    {
        return BigDoors.get().getDatabaseManager()
                       .deleteDoor(door, getCommandSender().getPlayer().orElse(null))
                       .thenApply(this::handleDatabaseActionResult);
    }
}
