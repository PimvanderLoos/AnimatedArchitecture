package nl.pim16aap2.bigdoors.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.core.api.IBigDoorsPlatformProvider;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.text.TextType;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to restart BigDoors.
 *
 * @author Pim
 */
@ToString
@Flogger
public class Restart extends BaseCommand
{
    private final IBigDoorsPlatformProvider platformProvider;

    @AssistedInject //
    Restart(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        IBigDoorsPlatformProvider platformProvider)
    {
        super(commandSender, localizer, textFactory);
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

    private void restartPlatform(IBigDoorsPlatform platform)
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
         *     The {@link ICommandSender} responsible for restarting BigDoors.
         * @return See {@link BaseCommand#run()}.
         */
        Restart newRestart(ICommandSender commandSender);
    }
}
