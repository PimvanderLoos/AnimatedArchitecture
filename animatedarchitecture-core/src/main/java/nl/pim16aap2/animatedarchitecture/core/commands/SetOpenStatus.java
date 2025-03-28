package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening status of structures.
 */
@ToString(callSuper = true)
public class SetOpenStatus extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_OPEN_STATUS;

    private static final List<Property<?>> REQUIRED_PROPERTIES = List.of(Property.OPEN_STATUS);

    private final boolean isOpen;

    @AssistedInject
    SetOpenStatus(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        @Assisted("isOpen") boolean isOpen,
        @Assisted("sendUpdatedInfo") boolean sendUpdatedInfo,
        IExecutor executor,
        CommandFactory commandFactory)
    {
        super(
            commandSender,
            executor,
            structureRetriever,
            StructureAttribute.OPEN_STATUS,
            sendUpdatedInfo,
            commandFactory
        );
        this.isOpen = isOpen;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected void handleDatabaseActionSuccess(@Nullable Structure retrieverResult)
    {
        final var desc = getRetrievedStructureDescription(retrieverResult);
        getCommandSender().sendSuccess(
            "commands.set_open_status.success",
            arg -> arg.highlight(desc.localizedTypeName()),
            arg -> arg.highlight(desc.id())
        );
    }

    @Override
    protected void notifyMissingProperties(Structure structure)
    {
        getCommandSender().sendError(
            "commands.set_open_status.error.missing_property",
            arg -> arg.localizedHighlight(structure),
            arg -> arg.highlight(structure.getNameAndUid())
        );
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        final @Nullable Boolean oldStatus = structure.setPropertyValue(Property.OPEN_STATUS, isOpen).value();
        if (oldStatus == null || oldStatus != isOpen)
        {
            // The open status has changed, so we need to update the database and inform the user.
            return structure
                .syncData()
                .thenAccept(result -> handleDatabaseActionResult(result, structure))
                .thenRunAsync(() -> sendUpdatedInfo(structure), executor.getVirtualExecutor());
        }

        // The open status has not changed, so we inform the user.
        final String isOpenKey =
            isOpen ?
                "constants.open_status.open" :
                "constants.open_status.closed";

        getCommandSender().sendError(
            "commands.set_open_status.error.status_not_changed",
            arg -> arg.localizedHighlight(structure),
            arg -> arg.highlight(structure.getNameAndUid()),
            arg -> arg.localizedHighlight(isOpenKey)
        );

        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected List<Property<?>> getRequiredProperties()
    {
        return REQUIRED_PROPERTIES;
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetOpenStatus} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing open status of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link Structure} for which the open status will be
         *     modified.
         * @param isOpen
         *     The new open status of the structure.
         * @param sendUpdatedInfo
         *     True to send the updated info text to the user after the command has been executed.
         * @return See {@link BaseCommand#run()}.
         */
        SetOpenStatus newSetOpenStatus(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            @Assisted("isOpen") boolean isOpen,
            @Assisted("sendUpdatedInfo") boolean sendUpdatedInfo
        );

        /**
         * Creates (but does not execute!) a new {@link SetOpenStatus} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing open status of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link Structure} for which the open status will be
         *     modified.
         * @param isOpen
         *     The new open status of the structure.
         * @return See {@link BaseCommand#run()}.
         */
        default SetOpenStatus newSetOpenStatus(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            boolean isOpen)
        {
            return newSetOpenStatus(commandSender, structureRetriever, isOpen, false);
        }
    }
}
