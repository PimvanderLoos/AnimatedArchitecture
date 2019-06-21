package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

public class SubCommandListDoors extends SubCommand
{
    protected static final String help = "Returns a list of all your doors";
    protected static final String argsHelp = "[doorName]";
    protected static final int minArgCount = 1;
    protected static final CommandData command = CommandData.LISTDOORS;

    public SubCommandListDoors(final BigDoors plugin, final CommandManager commandManager)
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
            builder.append(door.getBasicInfo() + "\n");
        plugin.getMyLogger().returnToSender(sender, null, builder.toString());
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        ArrayList<DoorBase> doors = new ArrayList<>();
        String name = args.length == minArgCount + 1 ? args[minArgCount] : null;
        if (sender instanceof Player)
            doors.addAll(plugin.getDatabaseManager().getDoors(((Player) sender).getUniqueId().toString(), name));
        else if (name != null)
        {
            doors.addAll(plugin.getDatabaseManager().getDoors(name));
            // If no door with the provided name could be found, list all doors owned by the player with that name instead.
            if (doors.size() == 0)
            {
                UUID playerUUID = plugin.getDatabaseManager().getPlayerUUIDFromString(name);
                if (playerUUID == null)
                    return true;
                doors.addAll(plugin.getDatabaseManager().getDoors(playerUUID.toString(), null));
            }
        }
        else
            return false;
        return execute(sender, doors);
    }
}
