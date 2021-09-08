package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

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
    protected Confirm(ICommandSender commandSender, CommandContext context)
    {
        super(commandSender, context);
    }

    /**
     * Runs the {@link Confirm} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to confirm any active processes.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context)
    {
        return new Confirm(commandSender, context).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.CONFIRM;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        final var toolUser = context.getToolUserManager().getToolUser(((IPPlayer) getCommandSender()).getUUID());
        if (toolUser.isPresent())
            toolUser.get().handleInput(true);
        else
            getCommandSender().sendMessage(localizer.getMessage("commands.confirm.error.no_confirmation_request"));

        return CompletableFuture.completedFuture(true);
    }
}
