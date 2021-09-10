package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
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
    private final IBigDoorsPlatform platform;

    @AssistedInject //
    Version(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
            IBigDoorsPlatform platform)
    {
        super(commandSender, logger, localizer);
        this.platform = platform;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.VERSION;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        getCommandSender().sendMessage(platform.getVersion());
        return CompletableFuture.completedFuture(true);
    }

    @AssistedFactory
    interface Factory
    {
        /**
         * Creates (but does not execute!) a new {@link Version} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for executing the command and the target for sending the message
         *     containing the current version.
         * @return See {@link BaseCommand#run()}.
         */
        Version newVersion(ICommandSender commandSender);
    }
}
