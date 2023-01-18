package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.debugging.DebugReporter;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;

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
    private final IMessagingInterface messagingInterface;
    private final DebugReporter debugReporter;

    @AssistedInject //
    Debug(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        IMessagingInterface messagingInterface, DebugReporter debugReporter)
    {
        super(commandSender, localizer, textFactory);
        this.messagingInterface = messagingInterface;
        this.debugReporter = debugReporter;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.DEBUG;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(PermissionsStatus permissions)
    {
        return CompletableFuture.runAsync(this::postDebugMessage).thenApply(val -> true);
    }

    private void postDebugMessage()
    {
        messagingInterface.writeToConsole(Level.INFO, debugReporter.getDebugReport());
        getCommandSender().sendSuccess(textFactory, localizer.getMessage("commands.debug.success"));
    }

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
        Debug newDebug(ICommandSender commandSender);
    }
}
