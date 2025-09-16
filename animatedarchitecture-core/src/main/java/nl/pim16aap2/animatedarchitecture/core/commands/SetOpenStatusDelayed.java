package nl.pim16aap2.animatedarchitecture.core.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;

import java.util.concurrent.CompletableFuture;

/**
 * Delayed version of {@link SetOpenStatus}.
 * <p>
 * This command is used to set the open status of a structure.
 * <p>
 * The open status can be provided as delayed input.
 */
@Singleton
@ToString(callSuper = true)
public class SetOpenStatusDelayed extends DelayedCommand<Boolean>
{
    @Inject
    public SetOpenStatusDelayed(Context context, DelayedCommandInputRequest.IFactory<Boolean> inputRequestFactory)
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
        ICommandSender commandSender,
        StructureRetriever structureRetriever,
        Boolean isOpen)
    {
        return commandFactory.get().newSetOpenStatus(commandSender, structureRetriever, isOpen).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, StructureRetriever structureRetriever)
    {
        return commandSender.getPersonalizedLocalizer().getMessage("commands.set_open_status.delayed.init");
    }
}
