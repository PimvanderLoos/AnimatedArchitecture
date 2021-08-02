package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.util.delayedinput.DelayedInputRequest;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Implements the command to specify a door for the {@link DoorSpecificationManager}.
 *
 * @author Pim
 */
@ToString
public class Specify extends BaseCommand
{
    private final @NotNull String input;

    protected Specify(final @NotNull ICommandSender commandSender, final @NotNull String input)
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
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender,
                                                          final @NotNull String name)
    {
        return new Specify(commandSender, name).run();
    }

    @Override
    public @NotNull CommandDefinition getCommand()
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
    protected @NotNull CompletableFuture<Boolean> executeCommand(final @NotNull BooleanPair permissions)
    {
        if (!BigDoors.get().getDoorSpecificationManager().handleInput((IPPlayer) getCommandSender(), input))
            getCommandSender().sendMessage(BigDoors.get().getLocalizer()
                                                   .getMessage("commands.base.error.no_pending_process"));
        return CompletableFuture.completedFuture(true);
    }
}
