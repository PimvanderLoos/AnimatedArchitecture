package nl.pim16aap2.bigdoors.commands.subcommands;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;

public class SubCommandListDoors implements ISubCommand
{
    protected final BigDoors plugin;
    protected final CommandManager commandManager;

    private static final String name = "listdoors";
    private static final String permission = "bigdoors.user.listdoors";
    private static final String help = "Returns a list of all your doors";
    private static final String argsHelp = "[doorName]";
    private static final int minArgCount = 1;

    public SubCommandListDoors(final BigDoors plugin, final CommandManager commandManager)
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
            builder.append(door.getBasicInfo() + "\n");
        plugin.getMyLogger().returnToSender(sender, null, builder.toString());
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        ArrayList<Door> doors = new ArrayList<>();
        String name = args.length == minArgCount + 1 ? args[minArgCount] : null;
        if (sender instanceof Player)
            doors.addAll(plugin.getCommander().getDoors(((Player) sender).getUniqueId().toString(), name));
        else if (name != null)
        {
            doors.addAll(plugin.getCommander().getDoors(name));
            // If no door with the provided name could be found, list all doors owned by the player with that name instead.
            if (doors.size() == 0)
            {
                UUID playerUUID = plugin.getCommander().getPlayerUUIDFromString(name);
                if (playerUUID == null)
                    return true;
                doors.addAll(plugin.getCommander().getDoors(playerUUID.toString(), null));
            }
        }
        else
            return false;
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
