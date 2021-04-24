package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedInputRequest;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command to specify a door for the {@link DoorSpecificationManager}.
 *
 * @author Pim
 */
@ToString
public class Specify extends BaseCommand
{
    private final @NonNull String input;

    protected Specify(final @NonNull ICommandSender commandSender, final @NonNull String input)
    {
        super(commandSender);
        this.input = input;
    }

    /**
     * Runs the {@link Specify} command.
     *
     * @param commandSender The {@link ICommandSender} responsible specifying a door.
     * @param name          The name/index that specifies a door based on the {@link DelayedInputRequest} for the
     *                      command sender as registered by the {@link DoorSpecificationManager}.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NonNull CompletableFuture<Boolean> run(final @NonNull ICommandSender commandSender,
                                                          final @NonNull String name)
    {
        return new Specify(commandSender, name).run();
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.SPECIFY;
    }

    // TODO: Make it available for non-players as well.
    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        if (!BigDoors.get().getDoorSpecificationManager().handleInput((IPPlayer) getCommandSender(), input))
            // TODO: Localization
            getCommandSender().sendMessage("We are not currently waiting for your input!");
        return CompletableFuture.completedFuture(true);
    }
}
