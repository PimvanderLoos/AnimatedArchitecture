package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

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
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, messages.getString(Message.ERROR_NODOORSFOUND));
            return true;
        }
        StringBuilder builder = new StringBuilder();
        for (DoorBase door : doors)
            builder.append(door.getBasicInfo()).append("\n");
        plugin.getPLogger().sendMessageToTarget(sender, Level.INFO, builder.toString());
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
            throws CommandSenderNotPlayerException, CommandPermissionException
    {
        ArrayList<DoorBase> doors = new ArrayList<>();
        String name = args.length == minArgCount + 1 ? args[minArgCount] : null;
        if (sender instanceof Player)
            doors.addAll(plugin.getDatabaseManager().getDoors(((Player) sender).getUniqueId(), name)
                               .orElse(new ArrayList<>()));
        else if (name != null)
        {
            doors.addAll(plugin.getDatabaseManager().getDoors(name).orElse(new ArrayList<>()));
            // If no door with the provided name could be found, list all doors owned by the
            // player with that name instead.
            if (doors.size() == 0)
            {
                UUID playerUUID = plugin.getDatabaseManager().getPlayerUUIDFromString(name);
                if (playerUUID == null)
                    return true;
                doors.addAll(plugin.getDatabaseManager().getDoors(playerUUID).orElse(new ArrayList<>()));
            }
        }
        else
            return false;
        return execute(sender, doors);
    }
}
