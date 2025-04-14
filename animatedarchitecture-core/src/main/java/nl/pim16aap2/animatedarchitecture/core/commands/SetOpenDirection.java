package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAttribute;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetrieverFactory;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that changes the opening direction of structures.
 */
@ToString(callSuper = true)
public class SetOpenDirection extends StructureTargetCommand
{
    public static final CommandDefinition COMMAND_DEFINITION = CommandDefinition.SET_OPEN_DIRECTION;

    private final MovementDirection movementDirection;

    @AssistedInject
    SetOpenDirection(
        @Assisted ICommandSender commandSender,
        @Assisted StructureRetriever structureRetriever,
        @Assisted MovementDirection movementDirection,
        @Assisted boolean sendUpdatedInfo,
        IExecutor executor,
        CommandFactory commandFactory)
    {
        super(
            commandSender,
            executor,
            structureRetriever,
            StructureAttribute.OPEN_DIRECTION,
            sendUpdatedInfo,
            commandFactory
        );
        this.movementDirection = movementDirection;
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
            "commands.set_open_direction.success",
            arg -> arg.highlight(desc.localizedTypeName()),
            arg -> arg.highlight(desc.id())
        );
    }

    @Override
    protected CompletableFuture<?> performAction(Structure structure)
    {
        if (!structure.getType().isValidOpenDirection(movementDirection))
        {
            getCommandSender().sendError(
                "commands.set_open_direction.error.invalid_rotation",
                arg -> arg.localizedHighlight(movementDirection.getLocalizationKey()),
                arg -> arg.localizedHighlight(structure),
                arg -> arg.highlight(structure.getBasicInfo())
            );
            return CompletableFuture.completedFuture(null);
        }

        structure.setOpenDirection(movementDirection);
        return structure
            .syncData()
            .thenAccept(result -> handleDatabaseActionResult(result, structure))
            .thenRunAsync(() -> sendUpdatedInfo(structure), executor.getVirtualExecutor());
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetOpenDirection} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing open direction of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link Structure} for which the open direction will
         *     be modified.
         * @param movementDirection
         *     The new movement direction.
         * @param sendUpdatedInfo
         *     True to send the updated info text to the user after the command has been executed.
         * @return See {@link BaseCommand#run()}.
         */
        SetOpenDirection newSetOpenDirection(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            MovementDirection movementDirection,
            boolean sendUpdatedInfo
        );

        /**
         * Creates (but does not execute!) a new {@link SetOpenDirection} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for changing open direction of the structure.
         * @param structureRetriever
         *     A {@link StructureRetrieverFactory} representing the {@link Structure} for which the open direction will
         *     be modified.
         * @param movementDirection
         *     The new movement direction.
         * @return See {@link BaseCommand#run()}.
         */
        default SetOpenDirection newSetOpenDirection(
            ICommandSender commandSender,
            StructureRetriever structureRetriever,
            MovementDirection movementDirection)
        {
            return newSetOpenDirection(commandSender, structureRetriever, movementDirection, false);
        }
    }
}
