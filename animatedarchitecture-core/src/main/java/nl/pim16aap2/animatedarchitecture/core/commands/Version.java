package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that shows the {@link ICommandSender} the current version of the plugin that is running.
 */
@ToString(callSuper = true)
public class Version extends BaseCommand
{
    @ToString.Exclude
    private final IAnimatedArchitecturePlatformProvider platformProvider;

    @AssistedInject
    Version(
        @Assisted ICommandSender commandSender,
        IExecutor executor,
        IAnimatedArchitecturePlatformProvider platformProvider)
    {
        super(commandSender, executor);
        this.platformProvider = platformProvider;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.VERSION;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        final String version = platformProvider
            .getPlatform()
            .map(platform -> platform.getProjectVersion().toString())
            .orElse("ERROR");

        getCommandSender().sendSuccess(
            "commands.version.success",
            arg -> arg.highlight(version)
        );

        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
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
