package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
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
    public Delete(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever)
    {
        super(commandSender, doorRetriever);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.DELETE;
    }

    @Override
    protected boolean isAllowed(final @NonNull AbstractDoorBase door, final boolean bypassPermission)
    {
        if (!getCommandSender().isPlayer() || bypassPermission)
            return true;

        return getCommandSender()
            .getPlayer()
            .flatMap(door::getDoorOwner)
            .map(doorOwner -> doorOwner.getPermission() <= DoorAttribute.getPermissionLevel(DoorAttribute.DELETE))
            .orElse(false);
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door)
    {
        return BigDoors.get().getDatabaseManager().deleteDoor(door);
    }
}
