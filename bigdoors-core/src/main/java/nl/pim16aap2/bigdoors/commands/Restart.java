package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to restart BigDoors.
 *
 * @author Pim
 */
@ToString
public class Restart extends BaseCommand
{
    public Restart(final @NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.RESTART;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        BigDoors.get().restart();
        return CompletableFuture.completedFuture(true);
    }
}
