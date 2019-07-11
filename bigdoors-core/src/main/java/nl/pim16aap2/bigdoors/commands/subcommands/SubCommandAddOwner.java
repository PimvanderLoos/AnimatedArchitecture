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
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

public class SubCommandAddOwner extends SubCommand
{
    protected final String help = "Add another owner for a door.";
    protected final String argsHelp = "{doorUID/Name} <player> [permissionLevel]";
    protected final int minArgCount = 3;
    protected CommandData command = CommandData.ADDOWNER;

    public SubCommandAddOwner(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, DoorBase door, String playerArg, int permission)
            throws CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        UUID playerUUID = CommandManager.getPlayerFromArg(playerArg);

        if (sender instanceof Player && !plugin.getDatabaseManager()
                                               .hasPermissionForAction((Player) sender, door.getDoorUID(),
                                                                       DoorAttribute.ADDOWNER))
            throw new CommandActionNotAllowedException();

        if (plugin.getDatabaseManager().addOwner(door, playerUUID, permission))
        {
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       ChatColor.RED + plugin.getMessages().getString("COMMAND.AddOwner.Success"));
            return true;
        }
        plugin.getPLogger()
              .sendMessageToTarget(sender, Level.INFO,
                                   ChatColor.RED + plugin.getMessages().getString("COMMAND.AddOwner.Fail"));
        return false;

    }

    public int getPermissionFromArgs(CommandSender sender, String[] args, int pos)
    {
        int permission = 1;
        if (args.length >= pos)
            return permission;
        try
        {
            permission = CommandManager.getIntegerFromArg(args[pos]);
        }
        catch (Exception uncaught)
        {
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, ChatColor.RED + "\"" + args[pos] + "\" "
                    + plugin.getMessages().getString("GENERAL.COMMAND.InvalidPermissionValue"));
        }
        return permission;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
            throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException,
                   CommandActionNotAllowedException, IllegalArgumentException
    {
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
        return execute(sender, commandManager.getDoorFromArg(sender, args[1]), args[2],
                       getPermissionFromArgs(sender, args, 3));
    }
}
