package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorRetriever;

import java.util.concurrent.CompletableFuture;

@ToString
public class MovePowerBlock extends DoorTargetCommand
{
    public MovePowerBlock(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever)
    {
        super(commandSender, doorRetriever);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.MOVE_POWERBLOCK;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected boolean isAllowed(final @NonNull AbstractDoorBase door, boolean bypassPermission)
    {
        return hasAccessToAttribute(door, DoorAttribute.RELOCATE_POWERBLOCK, bypassPermission);
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> performAction(final @NonNull AbstractDoorBase door)
    {
        throw new UnsupportedOperationException("This command has not yet been implemented!");
    }
}
