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
 * Represents the command that is used to change the number of blocks a block will try to move.
 */
@ToString
public class SetBlocksToMove extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_BLOCKS_TO_MOVE;

    private static final List<Property<?>> REQUIRED_PROPERTIES = List.of(Property.BLOCKS_TO_MOVE);

    private final int blocksToMove;

    @AssistedInject
    SetBlocksToMove(
        @Assisted ICommandSender commandSender,
        ILocalizer localizer,
        ITextFactory textFactory,
        @Assisted StructureRetriever structureRetriever,
        @Assisted int blocksToMove)
    {
        super(commandSender, localizer, textFactory, structureRetriever, StructureAttribute.BLOCKS_TO_MOVE);
        this.blocksToMove = blocksToMove;
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
            localizer.getMessage("commands.set_blocks_to_move.success"),
            TextType.SUCCESS,
            arg -> arg.highlight(desc.localizedTypeName()),
            arg -> arg.highlight(desc.id()))
        );
    }

    @Override
    protected void notifyMissingProperties(Structure structure)
    {
        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.set_blocks_to_move.error.invalid_structure_type"),
            TextType.ERROR,
            arg -> arg.highlight(localizer.getStructureType(structure)),
            arg -> arg.highlight(structure.getBasicInfo()))
        );
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        final @Nullable Integer oldStatus = structure.setPropertyValue(Property.BLOCKS_TO_MOVE, blocksToMove).value();

        if (oldStatus == null || oldStatus != blocksToMove)
            return structure
                .syncData()
                .thenAccept(this::handleDatabaseActionResult);

        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.set_blocks_to_move.error.status_not_changed"),
            TextType.ERROR,
            arg -> arg.highlight(localizer.getStructureType(structure)),
            arg -> arg.highlight(structure.getNameAndUid()),
            arg -> arg.highlight(blocksToMove))
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
         * Creates (but does not execute!) a new {@link SetBlocksToMove} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing the blocks-to-move distance of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link Structure} for which the blocks-to-move
         *     distance will be modified.
         * @param blocksToMove
         *     The new blocks-to-move distance.
         * @return See {@link BaseCommand#run()}.
         */
        SetBlocksToMove newSetBlocksToMove(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            int blocksToMove
        );
    }
}
