package nl.pim16aap2.animatedarchitecture.core.commands;

import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

/**
 * Delayed version of {@link SetBlocksToMove}.
 * <p>
 * This command is used to set the amount of blocks a structure should move when it is moved.
 * <p>
 * The distance measured in blocks can be provided as delayed input.
 */
@Singleton
public class SetBlocksToMoveDelayed extends DelayedCommand<Integer>
{
    @Inject
    public SetBlocksToMoveDelayed(
        Context context,
        DelayedCommandInputRequest.IFactory<Integer> inputRequestFactory)
    {
        super(context, inputRequestFactory, Integer.class);
    }

    @Override
    protected CommandDefinition getCommandDefinition()
    {
        return SetBlocksToMove.COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<?> delayedInputExecutor(
        ICommandSender commandSender,
        StructureRetriever structureRetriever,
        Integer distance)
    {
        return commandFactory.get().newSetBlocksToMove(commandSender, structureRetriever, distance).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, StructureRetriever structureRetriever)
    {
        return localizer.getMessage("commands.set_blocks_to_move.delayed.init");
    }
}
