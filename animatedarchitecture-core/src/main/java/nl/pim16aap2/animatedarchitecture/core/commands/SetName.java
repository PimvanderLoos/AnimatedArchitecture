package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.exceptions.CommandExecutionException;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.creator.Creator;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the setName command, which is used to provide a name for a {@link ToolUser}.
 */
@ToString(callSuper = true)
public class SetName extends BaseCommand
{
    private final String name;

    @ToString.Exclude
    private final ToolUserManager toolUserManager;

    @AssistedInject
    SetName(
        @Assisted ICommandSender commandSender,
        @Assisted String name,
        IExecutor executor,
        ILocalizer localizer,
        ITextFactory textFactory,
        ToolUserManager toolUserManager)
    {
        super(commandSender, executor, localizer, textFactory);
        this.name = name;
        this.toolUserManager = toolUserManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.SET_NAME;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<?> executeCommand(@Nullable PermissionsStatus permissions)
    {
        final IPlayer player = getCommandSender().getPlayer().orElseThrow(IllegalStateException::new);
        final Optional<ToolUser> tu = toolUserManager.getToolUser(player);
        if (tu.isPresent() && tu.get() instanceof Creator creator)
            return creator.handleInput(name);

        getCommandSender().sendError(
            textFactory,
            localizer.getMessage("commands.base.error.no_pending_process")
        );
        return CompletableFuture.failedFuture(new CommandExecutionException(true, "No pending process."));
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link SetName} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for providing the name.
         * @param name
         *     The new name specified by the command sender.
         * @return See {@link BaseCommand#run()}.
         */
        SetName newSetName(ICommandSender commandSender, String name);
    }
}
