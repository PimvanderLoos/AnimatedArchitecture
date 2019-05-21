package nl.pim16aap2.bigdoors.commands.subcommands;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;

public class SubCommandListPlayerDoors implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String help = "Returns a list of all doors owned by a given player";
    private static final String argsHelp = "<player> [doorName]";
    private static final int minArgCount = 2;
    private static final CommandData command = CommandData.LISTPLAYERDOORS;

    public SubCommandListPlayerDoors(final BigDoors plugin, final CommandManager commandManager)
    {
        this.plugin = plugin;
        this.commandManager = commandManager;
    }

    public boolean execute(CommandSender sender, ArrayList<Door> doors)
    {
        if (doors.size() == 0)
        {
            plugin.getMyLogger().returnToSender(sender, null, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
            return true;
        }
        StringBuilder builder = new StringBuilder();
        for (Door door : doors)
            builder.append(door.getBasicInfo());
        plugin.getMyLogger().returnToSender(sender, null, builder.toString());
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException
    {
        ArrayList<Door> doors = new ArrayList<>();
        UUID playerUUID = CommandManager.getPlayerFromArg(args[0]);
        String name = args.length > 1 ? args[1] : null;
        doors.addAll(plugin.getCommander().getDoors(playerUUID.toString(), name));
        return execute(sender, doors);
    }

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
