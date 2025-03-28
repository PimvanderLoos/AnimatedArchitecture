package nl.pim16aap2.animatedarchitecture.core.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.managers.StructureSpecificationManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the cancel command, which cancels any processes waiting for user input (e.g. structure creation).
 */
@ToString(callSuper = true)
public class Cancel extends BaseCommand
{
    @ToString.Exclude
    private final ToolUserManager toolUserManager;

    @ToString.Exclude
    private final StructureSpecificationManager doorSpecificationManager;

    @AssistedInject
    Cancel(
        @Assisted ICommandSender commandSender,
        IExecutor executor,
        ToolUserManager toolUserManager,
        StructureSpecificationManager doorSpecificationManager)
    {
        super(commandSender, executor);
        this.toolUserManager = toolUserManager;
        this.doorSpecificationManager = doorSpecificationManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.CANCEL;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        getCommandSender().getPlayer().ifPresent(this::cancelPlayer);
        return CompletableFuture.completedFuture(null);
    }

    private void cancelPlayer(IPlayer player)
    {
        if (toolUserManager.cancelToolUser(player) || doorSpecificationManager.cancelRequest(player))
            getCommandSender().sendSuccess("commands.cancel.success");
        else
            getCommandSender().sendError("commands.cancel.no_process");
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link Cancel} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} for which to cancel any active processes.
         * @return See {@link BaseCommand#run()}.
         */
        Cancel newCancel(ICommandSender commandSender);
    }
}
