package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SetOpenDirectionDelayed extends DelayedCommand<RotateDirection>
{
    @Inject public SetOpenDirectionDelayed(
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
    protected CompletableFuture<?> delayedInputExecutor(
        ICommandSender commandSender, MovableRetriever movableRetriever, RotateDirection openDir)
    {
        return commandFactory.get().newSetOpenDirection(commandSender, movableRetriever, openDir).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, MovableRetriever movableRetriever)
    {
        return localizer.getMessage("commands.set_open_direction.delayed.init");
    }
}
