package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigotutil.DoorAttribute;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Level;

public class SubCommandRemoveOwner extends SubCommand
{
    protected static final String help = "Remove another owner for a door.";
    protected static final String helpArgs = "{door} <player>";
    protected static final int minArgCount = 3;
    protected static final CommandData command = CommandData.REMOVEOWNER;
    private int actualMinArgCount;

    public SubCommandRemoveOwner(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
        actualMinArgCount = getMinArgCount();
    }

    public boolean execute(CommandSender sender, DoorBase door, String playerArg)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        UUID playerUUID = CommandManager.getPlayerFromArg(playerArg);

        if (sender instanceof Player && plugin.getDatabaseManager().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.REMOVEOWNER))
            throw new CommandActionNotAllowedException();

        if (plugin.getDatabaseManager().removeOwner(door, playerUUID))
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
            if (cw != null && cw.getCommand().equals(getName()))
            {
                if (args.length == minArgCount)
                    return cw.executeCommand(args);
                cw.abortSilently();
            }
        }
        return execute(sender, commandManager.getDoorFromArg(sender, args[getMinArgCount() - 2]), args[getMinArgCount() - 1]);
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
    public int getMinArgCount()
    {
        return minArgCount;
    }

    @Override
    public CommandData getCommandData()
    {
        return command;
    }

    @Override
    public String getPermission()
    {
        return CommandData.getPermission(command);
    }

    @Override
    public String getName()
    {
        return CommandData.getCommandName(command);
    }
}
