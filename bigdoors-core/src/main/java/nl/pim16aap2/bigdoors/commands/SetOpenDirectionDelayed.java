package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SetOpenDirectionDelayed extends DelayedCommand<RotateDirection>
{
    @Inject SetOpenDirectionDelayed(
        Context context, DelayedCommandInputRequest.IFactory<RotateDirection> inputRequestFactory)
    {
        super(context, inputRequestFactory, RotateDirection.class);
    }

    @Override
    protected CommandDefinition getCommandDefinition()
    {
        return SetOpenDirection.COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<Boolean> delayedInputExecutor(
        ICommandSender commandSender, DoorRetriever doorRetriever, RotateDirection openDir)
    {
        return commandFactory.get().newSetOpenDirection(commandSender, doorRetriever, openDir).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        return localizer.getMessage("commands.set_open_direction.init");
    }
}
