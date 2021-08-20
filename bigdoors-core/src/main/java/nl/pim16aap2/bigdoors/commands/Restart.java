package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the command that is used to restart BigDoors.
 *
 * @author Pim
 */
@ToString
public class Restart extends BaseCommand
{
    protected Restart(ICommandSender commandSender)
    {
        super(commandSender);
    }

    /**
     * Runs the {@link Restart} command.
     *
     * @param commandSender The {@link ICommandSender} responsible for restarting BigDoors.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender)
    {
        return new Restart(commandSender).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.RESTART;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        BigDoors.get().restart();
        return CompletableFuture.completedFuture(true);
    }
}
