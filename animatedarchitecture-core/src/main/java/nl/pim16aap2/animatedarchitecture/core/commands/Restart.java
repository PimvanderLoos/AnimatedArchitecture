package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to restart AnimatedArchitecture.
 */
@ToString(callSuper = true)
@Flogger
public class Restart extends BaseCommand
{
    @ToString.Exclude
    private final IAnimatedArchitecturePlatformProvider platformProvider;

    @AssistedInject
    Restart(
        @Assisted ICommandSender commandSender,
        IExecutor executor,
        ILocalizer localizer,
        ITextFactory textFactory,
        IAnimatedArchitecturePlatformProvider platformProvider)
    {
        super(commandSender, executor, localizer, textFactory);
        this.platformProvider = platformProvider;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.RESTART;
    }

    private void onFail()
    {
        getCommandSender().sendError(textFactory, localizer.getMessage("commands.restart.error"));
        log.atSevere().log("Failed to restart plugin: No active platform! Did it start successfully?");
    }

    private void restartPlatform(IAnimatedArchitecturePlatform platform)
    {
        platform.restartPlugin();
        getCommandSender().sendMessage(textFactory, TextType.SUCCESS, localizer.getMessage("commands.restart.success"));
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
