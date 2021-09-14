package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the cancel command, which cancels any processes waiting for user input (e.g. door creation).
 *
 * @author Pim
 */
@ToString
public class Cancel extends BaseCommand
{
    private final ToolUserManager toolUserManager;
    private final DoorSpecificationManager doorSpecificationManager;

    @AssistedInject //
    Cancel(@Assisted ICommandSender commandSender, IPLogger logger,
           nl.pim16aap2.bigdoors.localization.ILocalizer localizer, ToolUserManager toolUserManager,
           DoorSpecificationManager doorSpecificationManager, CompletableFutureHandler handler)
    {
        super(commandSender, logger, localizer, handler);
        this.toolUserManager = toolUserManager;
        this.doorSpecificationManager = doorSpecificationManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.CANCEL;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        getCommandSender().getPlayer().ifPresent(this::cancelPlayer);
        return CompletableFuture.completedFuture(true);
    }

    private void cancelPlayer(IPPlayer player)
    {
        toolUserManager.getToolUser(player.getUUID()).ifPresent(ToolUser::shutdown);
        doorSpecificationManager.cancelRequest(player);
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
