package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the cancel command, which cancels any processes waiting for user input (e.g. door creation).
 *
 * @author Pim
 */
@ToString
public class Cancel extends BaseCommand
{
    public Cancel(final @NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.CANCEL;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        getCommandSender().getPlayer().ifPresent(this::cancelPlayer);
        return CompletableFuture.completedFuture(true);
    }

    private void cancelPlayer(final @NonNull IPPlayer player)
    {
        BigDoors.get().getToolUserManager().getToolUser(player.getUUID()).ifPresent(ToolUser::shutdown);
        BigDoors.get().getDoorSpecificationManager().cancelRequest(player);
    }
}
