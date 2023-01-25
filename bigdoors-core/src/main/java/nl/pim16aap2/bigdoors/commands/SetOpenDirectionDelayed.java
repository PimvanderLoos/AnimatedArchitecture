package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SetOpenDirectionDelayed extends DelayedCommand<MovementDirection>
{
    @Inject public SetOpenDirectionDelayed(
        Context context, DelayedCommandInputRequest.IFactory<MovementDirection> inputRequestFactory)
    {
        super(context, inputRequestFactory, MovementDirection.class);
    }

    @Override
    protected CommandDefinition getCommandDefinition()
    {
        return SetOpenDirection.COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<?> delayedInputExecutor(
        ICommandSender commandSender, MovableRetriever movableRetriever, MovementDirection openDir)
    {
        return commandFactory.get().newSetOpenDirection(commandSender, movableRetriever, openDir).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, MovableRetriever movableRetriever)
    {
        return localizer.getMessage("commands.set_open_direction.delayed.init");
    }
}
