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
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class SubCommandSetBlocksToMove extends SubCommand
{
    protected static final String help = "Change the number of blocks the door will attempt to move in the provided direction";
    protected static final String argsHelp = "{doorUID/Name} <blocks>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.SETBLOCKSTOMOVE;

    public SubCommandSetBlocksToMove(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, DoorBase door, String blocksToMoveArg)
            throws CommandActionNotAllowedException, IllegalArgumentException
    {
        if (sender instanceof Player && !plugin.getDatabaseManager()
                                               .hasPermissionForAction((Player) sender, door.getDoorUID(),
                                                                       DoorAttribute.BLOCKSTOMOVE))
            throw new CommandActionNotAllowedException();

        int blocksToMove = CommandManager.getIntegerFromArg(blocksToMoveArg);

        plugin.getDatabaseManager().setDoorBlocksToMove(door.getDoorUID(), blocksToMove);

        if (blocksToMove > 0)
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       plugin.getMessages().getString("COMMAND.SetBlocksToMove.Success") +
                                               blocksToMove);
        else
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       plugin.getMessages().getString("COMMAND.SetBlocksToMove.Disabled"));
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
            throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException,
                   CommandActionNotAllowedException, IllegalArgumentException
    {
        if (args.length < minArgCount)
            return false;

        if (sender instanceof Player)
        {
            WaitForCommand cw = plugin.isCommandWaiter((Player) sender);
            if (cw != null && cw.getCommand().equals(getName()))
            {
                if (args.length == minArgCount)
                    return cw.executeCommand(args);
                cw.abortSilently();
            }
        }
        return execute(sender, commandManager.getDoorFromArg(sender, args[1]), args[2]);
    }
}
