package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the setName command, which is used to provide a name for a {@link ToolUser}.
 *
 * @author Pim
 */
@ToString
public class SetName extends BaseCommand
{
    private final String name;

    protected SetName(ICommandSender commandSender, String name)
    {
        super(commandSender);
        this.name = name;
    }

    /**
     * Runs the {@link SetName} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} responsible for providing the name.
     * @param name
     *     The new name specified by the command sender.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, String name)
    {
        return new SetName(commandSender, name).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.SET_NAME;
    }

    @Override
    protected boolean availableForNonPlayers()
    {
        return false;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        final IPPlayer player = (IPPlayer) getCommandSender();
        final Optional<ToolUser> tu = BigDoors.get().getToolUserManager().getToolUser(player.getUUID());
        if (tu.isPresent() && tu.get() instanceof Creator)
            return CompletableFuture.completedFuture(tu.get().handleInput(name));

        getCommandSender().sendMessage(BigDoors.get().getLocalizer()
                                               .getMessage("commands.base.error.no_pending_process"));
        return CompletableFuture.completedFuture(true);
    }
}
