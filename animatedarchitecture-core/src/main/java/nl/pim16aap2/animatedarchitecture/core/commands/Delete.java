package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.exceptions.NoAccessToStructureCommandException;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to delete structures.
 */
@ToString(callSuper = true)
public class Delete extends StructureTargetCommand
{
    @ToString.Exclude
    private final DatabaseManager databaseManager;

    @AssistedInject
    Delete(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        IExecutor executor,
        DatabaseManager databaseManager)
    {
        super(commandSender, executor, structureRetriever, StructureAttribute.DELETE);
        this.databaseManager = databaseManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.DELETE;
    }

    @Override
    protected void isAllowed(Structure structure, boolean bypassPermission)
    {
        if (hasAccessToAttribute(structure, StructureAttribute.DELETE, bypassPermission))
            return;

        getCommandSender().sendError(
            "commands.delete.error.not_allowed",
            arg -> arg.localizedHighlight(structure)
        );

        throw new NoAccessToStructureCommandException(true);
    }

    @Override
    protected void handleDatabaseActionSuccess(@Nullable Structure retrieverResult)
    {
        final var desc = getRetrievedStructureDescription(retrieverResult);
        getCommandSender().sendSuccess(
            "commands.delete.success",
            arg -> arg.highlight(desc.localizedTypeName()),
            arg -> arg.highlight(desc.id())
        );
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        return databaseManager
            .deleteStructure(structure, getCommandSender().getPlayer().orElse(null))
            .thenAccept(result -> handleDatabaseActionResult(result, structure));
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
         *     A {@link StructureRetrieverFactory} representing the {@link Structure} which will be targeted for
         *     deletion.
         * @return See {@link BaseCommand#run()}.
         */
        Delete newDelete(ICommandSender commandSender, StructureRetriever structureRetriever);
    }
}
