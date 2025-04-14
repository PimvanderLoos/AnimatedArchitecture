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
 * Represents the command that is used to change the number of blocks a block will try to move.
 */
@ToString(callSuper = true)
public class SetBlocksToMove extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_BLOCKS_TO_MOVE;

    private static final List<Property<?>> REQUIRED_PROPERTIES = List.of(Property.BLOCKS_TO_MOVE);

    private final int blocksToMove;

    @AssistedInject
    SetBlocksToMove(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        @Assisted int blocksToMove,
        IExecutor executor)
    {
        super(commandSender, executor, structureRetriever, StructureAttribute.BLOCKS_TO_MOVE);
        this.blocksToMove = blocksToMove;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return COMMAND_DEFINITION;
    }

    @Override
    protected void handleDatabaseActionSuccess(@org.jetbrains.annotations.Nullable Structure retrieverResult)
    {
        final var desc = getRetrievedStructureDescription(retrieverResult);

        getCommandSender().sendSuccess(
            "commands.set_blocks_to_move.success",
            arg -> arg.highlight(desc.localizedTypeName()),
            arg -> arg.highlight(desc.id())
        );
    }

    @Override
    protected void notifyMissingProperties(Structure structure)
    {
        getCommandSender().sendError(
            "commands.set_blocks_to_move.error.invalid_structure_type",
            arg -> arg.localizedHighlight(structure),
            arg -> arg.highlight(structure.getBasicInfo())
        );
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        final @Nullable Integer oldStatus = structure.setPropertyValue(Property.BLOCKS_TO_MOVE, blocksToMove).value();

        if (oldStatus == null || oldStatus != blocksToMove)
            return structure
                .syncData()
                .thenAccept(result -> handleDatabaseActionResult(result, structure));

        getCommandSender().sendError(
            "commands.set_blocks_to_move.error.status_not_changed",
            arg -> arg.localizedHighlight(structure),
            arg -> arg.highlight(structure.getNameAndUid()),
            arg -> arg.highlight(blocksToMove)
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
