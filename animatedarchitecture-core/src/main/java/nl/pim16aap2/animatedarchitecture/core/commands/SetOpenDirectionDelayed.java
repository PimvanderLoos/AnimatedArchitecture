package nl.pim16aap2.animatedarchitecture.core.commands;

import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

/**
 * Delayed version of {@link SetOpenDirection}.
 * <p>
 * This command is used to set the opening direction of a structure.
 * <p>
 * The opening direction can be provided as delayed input.
 */
@Singleton
@ToString(callSuper = true)
public class SetOpenDirectionDelayed extends DelayedCommand<MovementDirection>
{
    @Inject
    public SetOpenDirectionDelayed(
        Context context,
        DelayedCommandInputRequest.IFactory<MovementDirection> inputRequestFactory)
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
        ICommandSender commandSender,
        StructureRetriever structureRetriever,
        MovementDirection openDir)
    {
        return commandFactory.get().newSetOpenDirection(commandSender, structureRetriever, openDir).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, StructureRetriever structureRetriever)
    {
        return commandSender.getPersonalizedLocalizer().getMessage("commands.set_open_direction.delayed.init");
    }
}
