package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.MovableSpecificationManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the cancel command, which cancels any processes waiting for user input (e.g. movable creation).
 *
 * @author Pim
 */
@ToString
public class Cancel extends BaseCommand
{
    private final ToolUserManager toolUserManager;
    private final MovableSpecificationManager doorSpecificationManager;

    @AssistedInject //
    Cancel(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        ToolUserManager toolUserManager, MovableSpecificationManager doorSpecificationManager)
    {
        super(commandSender, localizer, textFactory);
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

    private void cancelPlayer(IPPlayer player)
    {
        if (toolUserManager.cancelToolUser(player) || doorSpecificationManager.cancelRequest(player))
            getCommandSender().sendSuccess(textFactory, localizer.getMessage("commands.cancel.success"));
        else
            getCommandSender().sendError(textFactory, localizer.getMessage("commands.cancel.no_process"));
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
