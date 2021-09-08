package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.api.DebugReporter;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Represents the debug command. This command is used to retrieve debug information, the specifics of which are left to
 * the currently registered platform. See {@link DebugReporter}.
 *
 * @author Pim
 */
@ToString
public class Debug extends BaseCommand
{
    protected Debug(ICommandSender commandSender, CommandContext context)
    {
        super(commandSender, context);
    }

    /**
     * Runs the {@link Debug} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} responsible for the execution of this command.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context)
    {
        return new Debug(commandSender, context).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.DEBUG;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        return CompletableFuture.runAsync(this::postDebugMessage).thenApply(val -> true);
    }

    private void postDebugMessage()
    {
        context.getMessagingInterface().writeToConsole(Level.INFO, context.getDebugReporter().getDump());
    }
}
