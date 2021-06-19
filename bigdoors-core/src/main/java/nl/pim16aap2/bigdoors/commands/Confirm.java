package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the confirm command, which is used to confirm actions BigDoors is waiting on.
 * <p>
 * For example, when buying something, the process might require the user to confirm that they agree to the
 * transaction.
 *
 * @author Pim
 */
@ToString
public class Confirm extends BaseCommand
{
    protected Confirm(final @NotNull ICommandSender commandSender)
    {
        super(commandSender);
    }

    /**
     * Runs the {@link Confirm} command.
     *
     * @param commandSender The {@link ICommandSender} for which to confirm any active processes.
     * @return See {@link BaseCommand#run()}.
     */
    public static @NotNull CompletableFuture<Boolean> run(final @NotNull ICommandSender commandSender)
    {
        return new Confirm(commandSender).run();
    }

    @Override
    public @NotNull CommandDefinition getCommand()
    {
        return CommandDefinition.CONFIRM;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected @NotNull CompletableFuture<Boolean> executeCommand(final @NotNull BooleanPair permissions)
    {
        val toolUser = BigDoors.get().getToolUserManager().getToolUser(((IPPlayer) getCommandSender()).getUUID());
        if (toolUser.isPresent())
            toolUser.get().handleInput(true);
        else
            // TODO: Localization.
            getCommandSender().sendMessage("Nothing to confirm!");

        return CompletableFuture.completedFuture(true);
    }
}
