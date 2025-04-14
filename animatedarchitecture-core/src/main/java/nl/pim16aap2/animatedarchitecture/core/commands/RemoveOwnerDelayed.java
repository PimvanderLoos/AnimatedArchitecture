package nl.pim16aap2.animatedarchitecture.core.commands;

import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.structures.retriever.StructureRetriever;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

/**
 * Delayed version of {@link RemoveOwner}.
 * <p>
 * This command is used to remove a player as a co-owner from a structure.
 * <p>
 * The target player can be provided as delayed input.
 */
@Singleton
@ToString(callSuper = true)
public class RemoveOwnerDelayed extends DelayedCommand<IPlayer>
{
    @Inject
    RemoveOwnerDelayed(Context context, DelayedCommandInputRequest.IFactory<IPlayer> inputRequestFactory)
    {
        super(context, inputRequestFactory, IPlayer.class);
    }

    @Override
    protected CommandDefinition getCommandDefinition()
    {
        return RemoveOwner.COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<?> delayedInputExecutor(
        ICommandSender commandSender,
        StructureRetriever structureRetriever,
        IPlayer targetPlayer)
    {
        return commandFactory.get().newRemoveOwner(commandSender, structureRetriever, targetPlayer).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, StructureRetriever structureRetriever)
    {
        return commandSender.getPersonalizedLocalizer().getMessage("commands.remove_owner.delayed.init");
    }
}
