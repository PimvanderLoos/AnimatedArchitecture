package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.moveblocks.MovableActivityManager;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command used to stop all active movables.
 *
 * @author Pim
 */
@ToString
public class StopMovables extends BaseCommand
{
    private final MovableActivityManager movableActivityManager;

    @AssistedInject //
    StopMovables(
        @Assisted ICommandSender commandSender, ILocalizer localizer, ITextFactory textFactory,
        MovableActivityManager movableActivityManager)
    {
        super(commandSender, localizer, textFactory);
        this.movableActivityManager = movableActivityManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.STOP_MOVABLES;
    }

    @Override
    protected CompletableFuture<?> executeCommand(PermissionsStatus permissions)
    {
        movableActivityManager.stopMovables();
        getCommandSender().sendSuccess(textFactory, localizer.getMessage("commands.stop_movables.success"));
        return CompletableFuture.completedFuture(null);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link StopMovables} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for stopping all active movables.
         * @return See {@link BaseCommand#run()}.
         */
        StopMovables newStopMovables(ICommandSender commandSender);
    }
}
