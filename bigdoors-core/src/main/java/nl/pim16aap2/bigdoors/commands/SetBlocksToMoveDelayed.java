package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SetBlocksToMoveDelayed extends DelayedCommand<Integer>
{
    @Inject public SetBlocksToMoveDelayed(
        Context context, DelayedCommandInputRequest.IFactory<Integer> inputRequestFactory)
    {
        super(context, inputRequestFactory, Integer.class);
    }

    @Override
    protected CommandDefinition getCommandDefinition()
    {
        return SetBlocksToMove.COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> delayedInputExecutor(
        ICommandSender commandSender, MovableRetriever movableRetriever, Integer distance)
    {
        return commandFactory.get().newSetBlocksToMove(commandSender, movableRetriever, distance).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, MovableRetriever movableRetriever)
    {
        return localizer.getMessage("commands.set_blocks_to_move.delayed.init");
    }
}
