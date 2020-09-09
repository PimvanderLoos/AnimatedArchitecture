package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class SubCommandDelete extends SubCommand
{
    protected static final String help = "Delete the specified door";
    protected static final String argsHelp = "<doorUID/Name>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.DELETE;

    public SubCommandDelete(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, AbstractDoorBase door)
    {
        String name = door.getName();
        long doorUID = door.getDoorUID();
        DatabaseManager.get().deleteDoor(door);
        plugin.getPLogger().sendMessageToTarget(sender, Level.INFO,
                                                messages.getString(Message.COMMAND_DOOR_DELETE_SUCCESS, name,
                                                                   Long.toString(doorUID)));
        return true;
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException,
               CommandActionNotAllowedException
    {
        commandManager.getDoorFromArg(sender, args[getMinArgCount() - 1], cmd, args).whenComplete(
            (optionalDoor, throwable) -> optionalDoor.ifPresent(
                door ->
                {
                    if (sender instanceof Player &&
                        !Util.hasPermissionForAction(((Player) sender).getUniqueId(), door, DoorAttribute.DELETE))
                        commandManager.handleException(new CommandActionNotAllowedException(), sender, cmd, args);
                    else
                        execute(sender, door);
                }));
        return true;
    }
}
