package nl.pim16aap2.bigdoors.commands;

import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.DoorSpecificationManager;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;

import java.util.concurrent.CompletableFuture;

/**
 * Represents the cancel command, which cancels any processes waiting for user input (e.g. door creation).
 *
 * @author Pim
 */
@ToString
public class Cancel extends BaseCommand
{
    private final ToolUserManager toolUserManager;
    private final DoorSpecificationManager doorSpecificationManager;

    protected Cancel(ICommandSender commandSender, CommandContext context,
                     ToolUserManager toolUserManager, DoorSpecificationManager doorSpecificationManager)
    {
        super(commandSender, context);
        this.toolUserManager = toolUserManager;
        this.doorSpecificationManager = doorSpecificationManager;
    }

    /**
     * Runs the {@link Cancel} command.
     *
     * @param commandSender
     *     The {@link ICommandSender} for which to cancel any active processes.
     * @return See {@link BaseCommand#run()}.
     */
    public static CompletableFuture<Boolean> run(ICommandSender commandSender, CommandContext context,
                                                 ToolUserManager toolUserManager,
                                                 DoorSpecificationManager doorSpecificationManager)
    {
        return new Cancel(commandSender, context, toolUserManager, doorSpecificationManager).run();
    }

    @Override
    public CommandDefinition getCommand()
    {
        return CommandDefinition.CANCEL;
    }

    @Override
    protected CompletableFuture<Boolean> executeCommand(BooleanPair permissions)
    {
        getCommandSender().getPlayer().ifPresent(this::cancelPlayer);
        return CompletableFuture.completedFuture(true);
    }

    private void cancelPlayer(IPPlayer player)
    {
        toolUserManager.getToolUser(player.getUUID()).ifPresent(ToolUser::shutdown);
        doorSpecificationManager.cancelRequest(player);
    }
}
