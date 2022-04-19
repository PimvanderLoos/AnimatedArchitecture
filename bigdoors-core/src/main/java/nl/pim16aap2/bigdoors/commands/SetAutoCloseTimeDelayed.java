package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;

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
    protected CompletableFuture<Boolean> delayedInputExecutor(
        ICommandSender commandSender, DoorRetriever doorRetriever, Integer autoCloseTime)
    {
        return commandFactory.get().newSetAutoCloseTime(commandSender, doorRetriever, autoCloseTime).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        return localizer.getMessage("commands.set_auto_close_time.init");
    }
}
