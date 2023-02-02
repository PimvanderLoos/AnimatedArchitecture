package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.structureretriever.StructureRetriever;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class RemoveOwnerDelayed extends DelayedCommand<IPPlayer>
{
    @Inject RemoveOwnerDelayed(
        Context context, DelayedCommandInputRequest.IFactory<IPPlayer> inputRequestFactory)
    {
        super(context, inputRequestFactory, IPPlayer.class);
    }

    @Override
    protected CommandDefinition getCommandDefinition()
    {
        return RemoveOwner.COMMAND_DEFINITION;
    }

    @Override
    protected CompletableFuture<?> delayedInputExecutor(
        ICommandSender commandSender, StructureRetriever structureRetriever, IPPlayer targetPlayer)
    {
        return commandFactory.get().newRemoveOwner(commandSender, structureRetriever, targetPlayer).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, StructureRetriever structureRetriever)
    {
        return localizer.getMessage("commands.remove_owner.delayed.init");
    }
}
