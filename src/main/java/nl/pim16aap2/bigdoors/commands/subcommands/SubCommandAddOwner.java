package nl.pim16aap2.bigdoors.commands.subcommands;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.waitForCommand.WaitForCommand;

public class SubCommandAddOwner implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String name = "addowner";
    private static final String permission = "bigdoors.user.addowner";
    private static final String help = "Add another owner for a door.";
    private static final String argsHelp = "{doorUID/Name} <player> [permissionLevel]";
    private static final int minArgCount = 3;

    public SubCommandAddOwner(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public boolean execute(CommandSender sender, Door door, String playerArg, int permission)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        UUID playerUUID = CommandManager.getPlayerFromArg(playerArg);

        if (sender instanceof Player && plugin.getCommander().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.ADDOWNER))
            throw new CommandActionNotAllowedException();

        if (plugin.getCommander().addOwner(door, playerUUID, permission))
        {
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                plugin.getMessages().getString("COMMAND.AddOwner.Success"));
            return true;
        }
        plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                            plugin.getMessages().getString("COMMAND.AddOwner.Fail"));
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
            plugin.getMyLogger()
                .returnToSender(sender, ChatColor.RED,
                                "\"" + args[pos] + "\" " + plugin.getMessages().getString("GENERAL.COMMAND.InvalidPermissionValue"));
        }
        return permission;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException,
        CommandActionNotAllowedException, CommandInvalidVariableException
    {
        if (sender instanceof Player)
        {
            WaitForCommand cw = plugin.isCommandWaiter((Player) sender);
            if (cw != null && cw.getCommand().equals(name))
            {
                if (args.length == minArgCount)
                    return cw.executeCommand(args);
                cw.abortSilently();
            }
        }
        return execute(sender, commandManager.getDoorFromArg(sender, args[1]), args[2], getPermissionFromArgs(sender, args, 3));
    }



//        Door door = null;
//        if (sender instanceof Player)
//        {
//            Player player = (Player) sender;
//
//            WaitForCommand cw = plugin.isCommandWaiter(player);
//            if (cw != null && cw.getCommand().equals(name))
//            {
//                // If the plugin is waiting for this player to execute this command via the
//                // waiter system and the
//                // Number of arguments is correct, do that. If not, cancel that waiter and use
//                // this instead.
//                if (args.length == 2)
//                {
//                    cw.executeCommand(args);
//                    return true;
//                }
//                cw.abortSilently();
//            }
//
//            door = plugin.getCommander().getDoor(args[1], player);
//            if (door == null)
//            {
//                plugin.getMyLogger()
//                    .returnToSender(sender, Level.INFO, ChatColor.RED,
//                                    "\"" + args[1] + "\" " + plugin.getMessages().getString("GENERAL.InvalidDoorName"));
//                return true;
//            }
//
//            if (plugin.getCommander().hasPermissionForAction(player, door.getDoorUID(), DoorAttribute.ADDOWNER))
//                return true;
//        }
//        else
//            try
//            {
//                long doorUID = Long.parseLong(args[0]);
//                door = plugin.getCommander().getDoor(doorUID);
//            }
//            // If it can't convert to a long, get all doors from the player with the
//            // provided name.
//            // If there is more than one, tell the player that they are going to have to
//            // make a choice.
//            catch (NumberFormatException e)
//            {
//                plugin.getMyLogger().returnToSender(sender, null,
//                                                    "Failed to add owner. \"" + args[0] + "\" is not a valid doorUID.");
//                return false;
//            }
//
//        UUID playerUUID = plugin.getCommander().playerUUIDFromName(args[2]);
//        int permission = 1; // Default value
//        if (args.length == 4)
//            try
//            {
//                permission = Integer.parseInt(args[3]);
//            }
//            catch (Exception uncaught)
//            {
//                plugin.getMyLogger().returnToSender(sender, ChatColor.RED, "\"" + args[3] + "\" "
//                    + Messages.getString("GENERAL.COMMAND.InvalidPermissionValue"));
//            }
//
//        if (playerUUID != null)
//        {
//            if (plugin.getCommander().addOwner(playerUUID, door, permission))
//            {
//                plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
//                                                    plugin.getMessages().getString("COMMAND.AddOwner.Success"));
//                return true;
//            }
//            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
//                                                plugin.getMessages().getString("COMMAND.AddOwner.Fail"));
//            return false;
//        }
//        plugin.getMyLogger()
//            .returnToSender(sender, Level.INFO, ChatColor.RED,
//                            plugin.getMessages().getString("GENERAL.PlayerNotFound") + ": \"" + args[2] + "\"");
//        return false;
//    }

    @Override
    public String getHelp(CommandSender sender)
    {
        return help;
    }

    @Override
    public String getHelpArguments()
    {
        return argsHelp;
    }

    @Override
    public String getPermission()
    {
        return permission;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }
}
