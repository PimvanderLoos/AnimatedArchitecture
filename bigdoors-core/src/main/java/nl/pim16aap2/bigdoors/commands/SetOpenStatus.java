package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.structures.AbstractStructure;
import nl.pim16aap2.bigdoors.structures.StructureAttribute;
import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever;
import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetrieverFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening status of structures.
 *
 * @author Pim
 */
@ToString
public class SetOpenStatus extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_OPEN_STATUS;

    private final boolean isOpen;

    @AssistedInject //
    SetOpenStatus(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        @Assisted StructureRetriever structureRetriever, @Assisted boolean isOpen)
    {
        super(commandSender, localizer, textFactory, structureRetriever, StructureAttribute.OPEN_STATUS);
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
        getCommandSender().sendSuccess(textFactory, localizer.getMessage("commands.set_open_status.success",
                                                                         desc.typeName(), desc.id()));
    }

    @Override
    protected CompletableFuture<?> performAction(AbstractStructure structure)
    {
        if (structure.isOpen() == isOpen)
        {
            getCommandSender().sendError(
                textFactory,
                localizer.getMessage("commands.set_open_status.error.status_not_changed",
                                     localizer.getStructureType(structure), structure.getNameAndUid(),
                                     localizer.getMessage(isOpen ?
                                                          localizer.getMessage("constants.open_status.open") :
                                                          localizer.getMessage("constants.open_status.closed")
                                     )));
            return CompletableFuture.completedFuture(null);
        }

        structure.setOpen(isOpen);
        return structure.syncData().thenAccept(this::handleDatabaseActionResult);
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
         *     A {@link StructureRetrieverFactory} representing the {@link AbstractStructure} for which the open status
         *     will be modified.
         * @param isOpen
         *     The new open status of the structure.
         * @return See {@link BaseCommand#run()}.
         */
        SetOpenStatus newSetOpenStatus(
            ICommandSender commandSender, StructureRetriever structureRetriever, boolean isOpen);
    }
}
