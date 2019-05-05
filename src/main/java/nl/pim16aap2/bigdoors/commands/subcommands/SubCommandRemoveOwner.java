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
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.waitForCommand.WaitForCommand;

public class SubCommandRemoveOwner implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String name = "removeowner";
    private static final String permission = "bigdoors.user.removeOwner";
    private static final String help = "Remove another owner for a door.";
    private static final String helpArgs = "{door} <player>";
    private static final int minArgCount = 3;

    public SubCommandRemoveOwner(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public boolean execute(CommandSender sender, Door door, String playerArg)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        UUID playerUUID = commandManager.getPlayerFromArg(playerArg);

        if (sender instanceof Player && plugin.getCommander().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.REMOVEOWNER))
            throw new CommandActionNotAllowedException();

        if (plugin.getCommander().removeOwner(door, playerUUID))
        {
            plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                                plugin.getMessages().getString("COMMAND.RemoveOwner.Success"));
            return true;
        }
        plugin.getMyLogger().returnToSender(sender, Level.INFO, ChatColor.RED,
                                            plugin.getMessages().getString("COMMAND.RemoveOwner.Fail"));
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandInvalidVariableException, CommandPlayerNotFoundException, CommandActionNotAllowedException
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
        return execute(sender, commandManager.getDoorFromArg(sender, args[1]), args[2]);
    }

    @Override
    public String getHelp(CommandSender sender)
    {
        return help;
    }

    @Override
    public String getHelpArguments()
    {
        return helpArgs;
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
