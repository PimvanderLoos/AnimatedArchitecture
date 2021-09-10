package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
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
    private final DatabaseManager databaseManager;

    @AssistedInject //
    Delete(@Assisted ICommandSender commandSender, IPLogger logger,
           nl.pim16aap2.bigdoors.localization.ILocalizer localizer,
           @Assisted DoorRetriever.AbstractRetriever doorRetriever, DatabaseManager databaseManager)
    {
        super(commandSender, logger, localizer, doorRetriever, DoorAttribute.DELETE);
        this.databaseManager = databaseManager;
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
        return databaseManager.deleteDoor(door, getCommandSender().getPlayer().orElse(null))
                              .thenApply(this::handleDatabaseActionResult);
    }

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link Delete} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for deleting the door.
         * @param doorRetriever
         *     A {@link DoorRetriever} representing the {@link DoorBase} which will be targeted for deletion.
         * @return See {@link BaseCommand#run()}.
         */
        Delete newDelete(ICommandSender commandSender, DoorRetriever.AbstractRetriever doorRetriever);
    }
}
