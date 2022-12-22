package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatformProvider;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.text.TextType;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to restart BigDoors.
 *
 * @author Pim
 */
@ToString
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

    private void restartPlatform(IBigDoorsPlatform platform)
    {
        platform.restartPlugin();
        getCommandSender().sendMessage(textFactory, TextType.SUCCESS, localizer.getMessage("commands.restart.success"));
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(PermissionsStatus permissions)
    {
        platformProvider.getPlatform().ifPresent(this::restartPlatform);
        return CompletableFuture.completedFuture(true);
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
