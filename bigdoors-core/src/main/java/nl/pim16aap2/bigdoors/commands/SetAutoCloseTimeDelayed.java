package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SetAutoCloseTimeDelayed extends DelayedCommand<Integer>
{
    @Inject SetAutoCloseTimeDelayed(
        Context context, DelayedCommandInputRequest.IFactory<Integer> inputRequestFactory)
    {
        super(context, inputRequestFactory, Integer.class);
    }

    @Override
    protected CommandDefinition getCommandDefinition()
    {
        return SetAutoCloseTime.COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<?> delayedInputExecutor(
        ICommandSender commandSender, MovableRetriever movableRetriever, Integer autoCloseTime)
    {
        return commandFactory.get().newSetAutoCloseTime(commandSender, movableRetriever, autoCloseTime).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, MovableRetriever movableRetriever)
    {
        return localizer.getMessage("commands.set_auto_close_time.delayed.init");
    }
}
