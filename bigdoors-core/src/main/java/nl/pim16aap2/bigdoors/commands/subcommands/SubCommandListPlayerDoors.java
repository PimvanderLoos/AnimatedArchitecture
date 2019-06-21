package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.UUID;

public class SubCommandListPlayerDoors extends SubCommand
{
    protected static final String help = "Returns a list of all doors owned by a given player";
    protected static final String argsHelp = "<player> [doorName]";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.LISTPLAYERDOORS;

    public SubCommandListPlayerDoors(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, ArrayList<DoorBase> doors)
    {
        if (doors.size() == 0)
        {
            plugin.getMyLogger().returnToSender(sender, null, plugin.getMessages().getString("GENERAL.NoDoorsFound"));
            return true;
        }
        StringBuilder builder = new StringBuilder();
        for (DoorBase door : doors)
            builder.append(door.getBasicInfo());
        plugin.getMyLogger().returnToSender(sender, null, builder.toString());
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException
    {
        ArrayList<DoorBase> doors = new ArrayList<>();
        UUID playerUUID = CommandManager.getPlayerFromArg(args[0]);
        String name = args.length > 1 ? args[1] : null;
        doors.addAll(plugin.getDatabaseManager().getDoors(playerUUID.toString(), name));
        return execute(sender, doors);
    }
}
