package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.CustomLog;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.debugging.DebugReporter;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the debug command. This command is used to retrieve debug information, the specifics of which are left to
 * the currently registered platform. See {@link DebugReporter}.
 */
@CustomLog
@ToString(callSuper = true)
public class Debug extends BaseCommand
{
    @ToString.Exclude
    private final DebugReporter debugReporter;

    @AssistedInject
    Debug(
        @Assisted ICommandSender commandSender,
        IExecutor executor,
        DebugReporter debugReporter)
    {
        super(commandSender, executor);
        this.debugReporter = debugReporter;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.DEBUG;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        return CompletableFuture.runAsync(this::postDebugMessage, executor.getVirtualExecutor());
    }

    private void postDebugMessage()
    {
        log.atInfo().log("%s", debugReporter.getDebugReport());
        getCommandSender().sendSuccess("commands.debug.success");
    }

    /**
     * The factory interface for creating {@link Debug} commands.
     */
    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Debug} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for the execution of this command.
         * @return See {@link BaseCommand#run()}.
         */
        @SuppressWarnings("NullableProblems")
        Debug newDebug(ICommandSender commandSender);
    }
}
