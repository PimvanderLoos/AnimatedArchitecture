package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class SubCommandDelete extends SubCommand
{
    protected static final String help = "Delete the specified door";
    protected static final String argsHelp = "<doorUID/Name>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.DELETE;

    public SubCommandDelete(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, DoorBase door)
    {
        String name = door.getName();
        long doorUID = door.getDoorUID();
        plugin.getDatabaseManager().removeDoor(door.getDoorUID());
        plugin.getPLogger().sendMessageToTarget(sender, Level.INFO,
                                                messages.getString(Message.COMMAND_DOOR_DELETE_SUCCESS, name,
                                                                   Long.toString(doorUID)));
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
            throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException,
                   CommandActionNotAllowedException
    {
        DoorBase door = commandManager.getDoorFromArg(sender, args[getMinArgCount() - 1]);

        if (sender instanceof Player && !plugin.getDatabaseManager()
                                               .hasPermissionForAction((Player) sender, door.getDoorUID(),
                                                                       DoorAttribute.DELETE))
            throw new CommandActionNotAllowedException();
        return execute(sender, door);
    }
}
