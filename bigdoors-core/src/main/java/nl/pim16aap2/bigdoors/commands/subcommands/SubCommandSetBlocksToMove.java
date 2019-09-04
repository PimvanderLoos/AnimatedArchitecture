package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Level;

public class SubCommandSetBlocksToMove extends SubCommand
{
    protected static final String help = "Change the number of blocks the door will attempt to move in the provided direction";
    protected static final String argsHelp = "{doorUID/Name} <blocks>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.SETBLOCKSTOMOVE;

    public SubCommandSetBlocksToMove(final @NotNull BigDoors plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    private void sendResultMessage(final @NotNull CommandSender sender, final int blocksToMove)
    {
        plugin.getPLogger()
              .sendMessageToTarget(sender, Level.INFO,
                                   blocksToMove > 0 ?
                                   messages.getString(Message.COMMAND_BLOCKSTOMOVE_SUCCESS,
                                                      Integer.toString(blocksToMove)) :
                                   messages.getString(Message.COMMAND_BLOCKSTOMOVE_DISABLED));
    }

    public boolean execute(final @NotNull CommandSender sender, final @NotNull DoorBase door,
                           final @NotNull String blocksToMoveArg)
        throws IllegalArgumentException
    {
        int blocksToMove = CommandManager.getIntegerFromArg(blocksToMoveArg);
        if (!(sender instanceof Player))
        {
            plugin.getDatabaseManager().setDoorBlocksToMove(door.getDoorUID(), blocksToMove);
            sendResultMessage(sender, blocksToMove);
            return true;
        }

        final Player player = (Player) sender;
        plugin.getDatabaseManager()
              .hasPermissionForAction(player, door.getDoorUID(), DoorAttribute.BLOCKSTOMOVE).whenComplete(
            (isAllowed, throwable) ->
            {
                if (!isAllowed)
                {
                    commandManager.handleException(new CommandActionNotAllowedException(), sender, null, null);
                    return;
                }
                plugin.getDatabaseManager().setDoorBlocksToMove(door.getDoorUID(), blocksToMove);
                sendResultMessage(sender, blocksToMove);
            });
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException,
               CommandActionNotAllowedException, IllegalArgumentException
    {
        if (args.length < minArgCount)
            return false;

        Optional<WaitForCommand> commandWaiter = commandManager.isCommandWaiter(sender, getName());
        if (commandWaiter.isPresent())
            return commandManager.commandWaiterExecute(commandWaiter.get(), args, minArgCount);

        commandManager.getDoorFromArg(sender, args[1], cmd, args).whenComplete(
            (optionalDoorBase, throwable) -> optionalDoorBase.ifPresent(door -> execute(sender, door, args[2])));
        return true;
    }
}
