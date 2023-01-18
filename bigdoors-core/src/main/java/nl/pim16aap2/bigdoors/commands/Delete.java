package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movable.MovableAttribute;
import nl.pim16aap2.bigdoors.movable.MovableBase;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to delete movables.
 *
 * @author Pim
 */
@ToString
public class Delete extends MovableTargetCommand
{
    private final DatabaseManager databaseManager;

    @AssistedInject //
    Delete(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted MovableRetriever movableRetriever, DatabaseManager databaseManager)
    {
        super(commandSender, localizer, textFactory, movableRetriever, MovableAttribute.DELETE);
        this.databaseManager = databaseManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.DELETE;
    }

    @Override
    protected boolean isAllowed(AbstractMovable movable, boolean bypassPermission)
    {
        return hasAccessToAttribute(movable, MovableAttribute.DELETE, bypassPermission);
    }

    @Override
    protected void handleDatabaseActionSuccess()
    {
        final var desc = getRetrievedMovableDescription();
        getCommandSender().sendSuccess(textFactory,
                                       localizer.getMessage("commands.delete.success", desc.typeName(), desc.id()));
    }

    @Override
    protected CompletableFuture<Boolean> performAction(AbstractMovable movable)
    {
        return databaseManager.deleteMovable(movable, getCommandSender().getPlayer().orElse(null))
                              .thenApply(this::handleDatabaseActionResult);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Delete} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for deleting the movable.
         * @param movableRetriever
         *     A {@link MovableRetrieverFactory} representing the {@link MovableBase} which will be targeted for
         *     deletion.
         * @return See {@link BaseCommand#run()}.
         */
        Delete newDelete(ICommandSender commandSender, MovableRetriever movableRetriever);
    }
}
