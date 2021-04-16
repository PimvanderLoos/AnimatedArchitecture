package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

@ToString
public class Confirm extends BaseCommand
{
    public Confirm(final @NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.CONFIRM;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        if (!(getCommandSender() instanceof IPPlayer))
        {
            getCommandSender().sendMessage("Only players can use this command!");
            return CompletableFuture.completedFuture(true);
        }

        val toolUser = BigDoors.get().getToolUserManager().getToolUser(((IPPlayer) getCommandSender()).getUUID());
        if (toolUser.isPresent())
            toolUser.get().handleInput(true);
        else
            // TODO: Localization.
            getCommandSender().sendMessage("Nothing to confirm!");

        return CompletableFuture.completedFuture(true);
    }
}
