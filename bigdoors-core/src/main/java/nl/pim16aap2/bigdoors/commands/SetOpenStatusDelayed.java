package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SetOpenStatusDelayed extends DelayedCommand<Boolean>
{
    @Inject public SetOpenStatusDelayed(
        Context context, DelayedCommandInputRequest.IFactory<Boolean> inputRequestFactory)
    {
        super(context, inputRequestFactory, Boolean.class);
    }

    @Override
    protected CommandDefinition getCommandDefinition()
    {
        return SetOpenStatus.COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<?> delayedInputExecutor(
        ICommandSender commandSender, StructureRetriever structureRetriever, Boolean isOpen)
    {
        return commandFactory.get().newSetOpenStatus(commandSender, structureRetriever, isOpen).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, StructureRetriever structureRetriever)
    {
        return localizer.getMessage("commands.set_open_status.delayed.init");
    }
}
