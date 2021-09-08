package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
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
    protected StopDoors(ICommandSender commandSender, CommandContext context)
    {
        super(commandSender, context);
    }

    /**
     * Runs the {@link StopDoors} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} responsible for stopping all active doors.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context)
    {
        return new StopDoors(commandSender, context).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.STOP_DOORS;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        context.getDoorActivityManager().stopDoors();
        return CompletableFuture.completedFuture(true);
    }
}
