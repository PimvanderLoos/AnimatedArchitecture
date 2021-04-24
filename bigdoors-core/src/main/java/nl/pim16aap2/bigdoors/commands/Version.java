package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that shows the {@link ICommandSender} the current version of the plugin that is running.
 *
 * @author Pim
 */
@ToString
public class Version extends BaseCommand
{
    protected Version(final @NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    /**
     * Runs the {@link Version} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for executing the command and the target for sending
     *                      the message containing the current version.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender)
    {
        return new Version(commandSender).run();
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.VERSION;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        getCommandSender().sendMessage(BigDoors.get().getVersion());
        return CompletableFuture.completedFuture(true);
    }
}
