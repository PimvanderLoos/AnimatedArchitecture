package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command used to stop all active doors.
 *
 * @author Pim
 */
@ToString
public class StopDoors extends BaseCommand
{
    protected StopDoors(final @NotNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    /**
     * Runs the {@link StopDoors} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for stopping all active doors.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender)
    {
        return new StopDoors(commandSender).run();
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return CommandDefinition.STOP_DOORS;
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> executeCommand(final @NotNull BooleanPair permissions)
    {
        BigDoors.get().getDoorActivityManager().stopDoors();
        return CompletableFuture.completedFuture(true);
    }
}
