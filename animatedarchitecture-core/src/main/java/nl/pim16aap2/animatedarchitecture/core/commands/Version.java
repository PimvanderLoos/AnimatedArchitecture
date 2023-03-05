package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatform;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitecturePlatformProvider;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that shows the {@link ICommandSender} the current version of the plugin that is running.
 *
 * @author Pim
 */
@ToString
public class Version extends BaseCommand
{
    private final IAnimatedArchitecturePlatformProvider platformProvider;

    @AssistedInject //
    Version(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        IAnimatedArchitecturePlatformProvider platformProvider)
    {
        super(commandSender, localizer, textFactory);
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
        final String version =
            platformProvider.getPlatform().map(IAnimatedArchitecturePlatform::getVersionName).orElse("ERROR");

        getCommandSender().sendMessage(textFactory.newText().append(
            localizer.getMessage("commands.version.success"), TextType.SUCCESS,
            arg -> arg.highlight(version)));

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
