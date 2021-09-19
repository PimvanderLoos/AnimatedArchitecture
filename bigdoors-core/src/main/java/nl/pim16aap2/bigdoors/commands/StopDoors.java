package nl.pim16aap2.bigdoors.commands;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import lombok.ToString;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.moveblocks.DoorActivityManager;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command used to stop all active doors.
 *
 * @author Pim
 */
@ToString
public class StopDoors extends BaseCommand
{
    private final DoorActivityManager doorActivityManager;

    @AssistedInject //
    StopDoors(@Assisted ICommandSender commandSender, IPLogger logger, ILocalizer localizer,
              DoorActivityManager doorActivityManager)
    {
        super(commandSender, logger, localizer);
        this.doorActivityManager = doorActivityManager;
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.STOP_DOORS;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        doorActivityManager.stopDoors();
        return CompletableFuture.completedFuture(true);
    }

    @AssistedFactory
    interface IFactory
    {
        /**
         * Creates (but does not execute!) a new {@link StopDoors} command.
         *
         * @param commandSender
         *     The {@link ICommandSender} responsible for stopping all active doors.
         * @return See {@link BaseCommand#run()}.
         */
        StopDoors newStopDoors(ICommandSender commandSender);
    }
}
