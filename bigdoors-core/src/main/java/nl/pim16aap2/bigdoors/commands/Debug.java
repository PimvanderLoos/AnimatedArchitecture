package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.jetbrains.annotations.NotNull;

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
    protected Debug(final @NotNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    /**
     * Runs the {@link Debug} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for the execution of this command.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender)
    {
        return new Debug(commandSender).run();
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return CommandDefinition.DEBUG;
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> executeCommand(final @NotNull BooleanPair permissions)
    {
        return CompletableFuture.runAsync(this::postDebugMessage).thenApply(val -> true);
    }

    private void postDebugMessage()
    {
        BigDoors.get().getMessagingInterface().writeToConsole(Level.INFO, BigDoors.get().getDebugReporter().getDump());
    }
}
