package nl.pim16aap2.bigdoors.commands;

import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
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

    public Specify(final @NonNull ICommandSender commandSender, final @NonNull String input)
    {
        super(commandSender);
        this.input = input;
    }

    @Override
    public @NonNull CommandDefinition getCommand()
    {
        return CommandDefinition.SPECIFY;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected @NonNull CompletableFuture<Boolean> executeCommand(final @NonNull BooleanPair permissions)
    {
        if (!BigDoors.get().getDoorSpecificationManager().handleInput((IPPlayer) getCommandSender(), input))
        {
            // TODO: Localization
            getCommandSender().sendMessage("We are not currently waiting for your input!");
        }
        return CompletableFuture.completedFuture(true);
    }
}
