package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.CustomLog;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to restart AnimatedArchitecture.
 */
@ToString(callSuper = true)
@CustomLog
public class Restart extends BaseCommand
{
    @ToString.Exclude
    private final IAnimatedArchitecturePlatformProvider platformProvider;

    @AssistedInject
    Restart(
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
        return CommandDefinition.RESTART;
    }

    private void onFail()
    {
        getCommandSender().sendError("commands.restart.error");
        log.atError().log("Failed to restart plugin: No active platform! Did it start successfully?");
    }

    private void restartPlatform(IAnimatedArchitecturePlatform platform)
    {
        platform.restartPlugin();
        getCommandSender().sendSuccess("commands.restart.success");
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        platformProvider.getPlatform().ifPresentOrElse(this::restartPlatform, this::onFail);
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Restart} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for restarting AnimatedArchitecture.
         * @return See {@link BaseCommand#run()}.
         */
        Restart newRestart(ICommandSender commandSender);
    }
}
