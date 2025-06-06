package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the confirm command, which is used to confirm actions AnimatedArchitecture is waiting on.
 * <p>
 * For example, when buying something, the process might require the user to confirm that they agree to the
 * transaction.
 */
@ToString(callSuper = true)
public class Confirm extends BaseCommand
{
    @ToString.Exclude
    private final ToolUserManager toolUserManager;

    @AssistedInject
    Confirm(
        @Assisted ICommandSender commandSender,
        IExecutor executor,
        ToolUserManager toolUserManager)
    {
        super(commandSender, executor);
        this.toolUserManager = toolUserManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.CONFIRM;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        final var toolUser = toolUserManager.getToolUser(((IPlayer) getCommandSender()).getUUID());
        if (toolUser.isPresent())
            return toolUser.get().handleInput(true);

        getCommandSender().sendError("commands.confirm.error.no_confirmation_request");

        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Confirm} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} for which to confirm any active processes.
         * @return See {@link BaseCommand#run()}.
         */
        Confirm newConfirm(ICommandSender commandSender);
    }
}
