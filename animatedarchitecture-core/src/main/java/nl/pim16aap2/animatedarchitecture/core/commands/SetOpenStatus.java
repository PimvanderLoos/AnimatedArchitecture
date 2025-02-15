package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening status of structures.
 */
@ToString
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
        ILocalizer localizer,
        ITextFactory textFactory,
        CommandFactory commandFactory)
    {
        super(
            commandSender,
            localizer,
            textFactory,
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
    protected void handleDatabaseActionSuccess()
    {
        final var desc = getRetrievedStructureDescription();
        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.set_open_status.success"),
            TextType.SUCCESS,
            arg -> arg.highlight(desc.localizedTypeName()),
            arg -> arg.highlight(desc.id()))
        );
    }

    @Override
    protected void notifyMissingProperties(Structure structure)
    {
        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.set_open_status.error.missing_property"),
            TextType.ERROR,
            arg -> arg.highlight(localizer.getStructureType(structure)),
            arg -> arg.highlight(structure.getNameAndUid()))
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
                .thenAccept(this::handleDatabaseActionResult)
                .thenRunAsync(() -> sendUpdatedInfo(structure));
        }

        // The open status has not changed, so we inform the user.
        final String localizedIsOpen =
            isOpen ?
                localizer.getMessage("constants.open_status.open") :
                localizer.getMessage("constants.open_status.closed");

        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.set_open_status.error.status_not_changed"),
            TextType.ERROR,
            arg -> arg.highlight(localizer.getStructureType(structure)),
            arg -> arg.highlight(structure.getNameAndUid()),
            arg -> arg.highlight(localizedIsOpen))
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
