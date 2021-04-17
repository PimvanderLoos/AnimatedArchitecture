package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IBlocksToMoveArchetype;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.waitforcommand.WaitForCommand;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;

public class SubCommandSetBlocksToMove extends SubCommand
{
    protected static final String help = "Change the number of blocks the door will attempt to move in the provided direction";
    protected static final String argsHelp = "{doorUID/Name} <blocks>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.SETBLOCKSTOMOVE;

    public SubCommandSetBlocksToMove(final @NonNull BigDoorsSpigot plugin, final @NonNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    private void sendResultMessage(final @NonNull CommandSender sender, final int blocksToMove)
    {
        plugin.getPLogger()
              .sendMessageToTarget(sender, Level.INFO,
                                   blocksToMove > 0 ?
                                   messages.getString(Message.COMMAND_BLOCKSTOMOVE_SUCCESS,
                                                      Integer.toString(blocksToMove)) :
                                   messages.getString(Message.COMMAND_BLOCKSTOMOVE_DISABLED));
    }

    public boolean execute(final @NonNull CommandSender sender, final @NonNull AbstractDoorBase door,
                           final @NonNull String blocksToMoveArg)
        throws IllegalArgumentException
    {
        if (!(door instanceof IBlocksToMoveArchetype))
            throw new IllegalArgumentException(
                "Doors of type: " + door.getDoorType().toString() + " do not have the \"blocksToMove\" property!");

        IBlocksToMoveArchetype doorBTM = (IBlocksToMoveArchetype) door;

        int blocksToMove = CommandManager.getIntegerFromArg(blocksToMoveArg);
        if (!(sender instanceof Player))
        {
            doorBTM.setBlocksToMove(blocksToMove);
            door.syncData();
            sendResultMessage(sender, blocksToMove);
            return true;
        }

        if (!Util.hasPermissionForAction(((Player) sender).getUniqueId(), door, DoorAttribute.BLOCKS_TO_MOVE))
        {
            commandManager.handleException(new CommandActionNotAllowedException(), sender, null, null);
            return true;
        }

        doorBTM.setBlocksToMove(blocksToMove).syncData();
        sendResultMessage(sender, blocksToMove);

        return true;
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command cmd,
                             final @NonNull String label, final @NonNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException,
               CommandActionNotAllowedException, IllegalArgumentException
    {
        if (args.length < minArgCount)
            return false;

        Optional<WaitForCommand> commandWaiter = commandManager.isCommandWaiter(sender, getName());
        if (commandWaiter.isPresent())
            return commandManager.commandWaiterExecute(commandWaiter.get(), args, minArgCount);

        if (sender instanceof Player)
        {
            final @NonNull Optional<ToolUser> toolUser = BigDoors.get().getToolUserManager()
                                                                 .getToolUser(((Player) sender).getUniqueId());
            if (toolUser.isPresent())
            {
                OptionalInt btm = Util.parseInt(args[1]);
                if (btm.isEmpty())
                    return false;

                toolUser.get().handleInput(btm.getAsInt());
                return true;
            }
        }

        commandManager.getDoorFromArg(sender, args[1], cmd, args).whenComplete(
            (optionalDoorBase, throwable) -> optionalDoorBase.ifPresent(door -> execute(sender, door, args[2])));
        return true;
    }
}
