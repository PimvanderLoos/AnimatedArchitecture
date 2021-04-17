package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a command to list a number of doors matching a single {@link DoorRetriever}. This is basically only useful
 * for String-based lookups (as there aren't duplicate matches otherwise), but I don't judge.
 *
 * @author Pim
 */
@ToString
public class ListDoors extends BaseCommand
{
    final @NonNull DoorRetriever doorRetriever;

    public ListDoors(final @NonNull ICommandSender commandSender, final @NonNull DoorRetriever doorRetriever)
    {
        super(commandSender);
        this.doorRetriever = doorRetriever;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.LIST_DOORS;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        final @NonNull CompletableFuture<List<AbstractDoorBase>> doors;
        if (permissions.second || !(getCommandSender() instanceof IPPlayer))
            doors = doorRetriever.getDoors();
        else
            doors = doorRetriever.getDoors((IPPlayer) getCommandSender());

        return doors.thenAccept(this::sendDoorList).thenApply(val -> true);
    }

    private void sendDoorList(final @NonNull List<AbstractDoorBase> doors)
    {
        if (doors.isEmpty())
        {
            // TODO: Localization
            getCommandSender().sendMessage("No doors found!");
            return;
        }

        // TODO: Localization
        final StringBuilder sb = new StringBuilder("List of doors:\n");
        for (val door : doors)
            sb.append("  ").append(door.getBasicInfo()).append("\n");
        getCommandSender().sendMessage(sb.toString());
    }
}
