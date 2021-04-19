package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents the debug command. This command is used to retrieve debug information, the specifics of which are left to
 * the currently registered platform. See {@link BigDoors#getDebugReporter()}.
 *
 * @author Pim
 */
@ToString
public class Debug extends BaseCommand
{
    public Debug(final @NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.DEBUG;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        CompletableFuture.runAsync(this::postDebugMessage);
        return CompletableFuture.completedFuture(true);
    }

    private void postDebugMessage()
    {
        BigDoors.get().getMessagingInterface().writeToConsole(Level.INFO, BigDoors.get().getDebugReporter().getDump());
    }
}
