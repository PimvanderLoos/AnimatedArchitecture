package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a command to list a number of doors matching a single {@link DoorRetriever}. This is basically only useful
 * for String-based look-ups (as there aren't duplicate matches otherwise), but I don't judge.
 *
 * @author Pim
 */
@ToString
public class ListDoors extends BaseCommand
{
    private final DoorRetriever.AbstractRetriever doorRetriever;

    @AssistedInject //
    ListDoors(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
              @Assisted DoorRetriever.AbstractRetriever doorRetriever)
    {
        super(commandSender, logger, localizer);
        this.doorRetriever = doorRetriever;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.LIST_DOORS;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        final CompletableFuture<List<AbstractDoor>> doors;
        if (permissions.second || !(getCommandSender() instanceof IPPlayer))
            doors = doorRetriever.getDoors();
        else
            doors = doorRetriever.getDoors((IPPlayer) getCommandSender());

        return doors.thenAccept(this::sendDoorList).thenApply(val -> true);
    }

    private void sendDoorList(List<AbstractDoor> doors)
    {
        if (doors.isEmpty())
        {
            getCommandSender().sendMessage(localizer.getMessage("commands.list_doors.error.no_doors_found"));
            return;
        }

        final StringBuilder sb = new StringBuilder(
            localizer.getMessage("commands.list_doors.door_list_header")).append('\n');
        for (final var door : doors)
            sb.append("  ").append(door.getBasicInfo()).append('\n');
        getCommandSender().sendMessage(sb.toString());
    }

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link ListDoors} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for retrieving the information for the doors.
         *     <p>
         *     This is also the entity that will be informed about the doors that were found.
         * @param doorRetriever
         *     A {@link DoorRetriever} representing any number of {@link DoorBase}s.
         * @return See {@link BaseCommand#run()}.
         */
        ListDoors newListDoors(ICommandSender commandSender, DoorRetriever.AbstractRetriever doorRetriever);
    }
}
