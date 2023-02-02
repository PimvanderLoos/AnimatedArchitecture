package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structures.StructureAttribute;
import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever;
import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to delete structures.
 *
 * @author Pim
 */
@ToString
public class Delete extends StructureTargetCommand
{
    private final DatabaseManager databaseManager;

    @AssistedInject //
    Delete(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted StructureRetriever structureRetriever, DatabaseManager databaseManager)
    {
        super(commandSender, localizer, textFactory, structureRetriever, StructureAttribute.DELETE);
        this.databaseManager = databaseManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.DELETE;
    }

    @Override
    protected boolean isAllowed(AbstractStructure structure, boolean bypassPermission)
    {
        return hasAccessToAttribute(structure, StructureAttribute.DELETE, bypassPermission);
    }

    @Override
    protected void handleDatabaseActionSuccess()
    {
        final var desc = getRetrievedStructureDescription();
        getCommandSender().sendSuccess(textFactory,
                                       localizer.getMessage("commands.delete.success", desc.typeName(), desc.id()));
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractStructure structure)
    {
        return databaseManager.deleteStructure(structure, getCommandSender().getPlayer().orElse(null))
                              .thenAccept(this::handleDatabaseActionResult);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Delete} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for deleting the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link AbstractStructure} which will be targeted for
         *     deletion.
         * @return See {@link BaseCommand#run()}.
         */
        Delete newDelete(ICommandSender commandSender, StructureRetriever structureRetriever);
    }
}
