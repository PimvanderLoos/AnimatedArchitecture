package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandInvalidVariableException;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;

public class SubCommandInfo extends SubCommand
{
    protected static final String help = "Display info of a door.";
    protected static final String argsHelp = "<doorUID/Name>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.INFO;

    public SubCommandInfo(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, Door door)
    {
        if (sender instanceof Player && door.getPermission() >= 0
            && door.getPermission() > DoorAttribute.getPermissionLevel(DoorAttribute.INFO))
            return true;
        plugin.getMyLogger().returnToSender(sender, null, door.getFullInfo());
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandInvalidVariableException
    {
        Door door = plugin.getDatabaseManager().getDoor(args[minArgCount - 1], sender instanceof Player ? (Player) sender : null);
        if (door == null)
        {
            plugin.getMyLogger().returnToSender(sender, null, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
            return true;
        }

        return execute(sender, door);
    }
}
