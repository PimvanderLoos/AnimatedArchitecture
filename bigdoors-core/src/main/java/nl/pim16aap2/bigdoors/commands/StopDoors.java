package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
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
    protected StopDoors(final @NonNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    /**
     * Runs the {@link StopDoors} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for stopping all active doors.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender)
    {
        return new StopDoors(commandSender).run();
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.STOP_DOORS;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        BigDoors.get().getDoorActivityManager().stopDoors();
        return CompletableFuture.completedFuture(true);
    }
}
