package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;

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
    protected CompletableFuture<Boolean> delayedInputExecutor(
        ICommandSender commandSender, DoorRetriever doorRetriever, IPPlayer targetPlayer)
    {
        return commandFactory.get().newRemoveOwner(commandSender, doorRetriever, targetPlayer).run();
    }

    @Override
    protected String inputRequestMessage(ICommandSender commandSender, DoorRetriever doorRetriever)
    {
        return localizer.getMessage("commands.remove_owner.init");
    }
}
