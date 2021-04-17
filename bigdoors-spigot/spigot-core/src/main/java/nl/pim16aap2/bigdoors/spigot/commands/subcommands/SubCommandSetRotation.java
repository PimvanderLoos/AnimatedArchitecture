package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.logging.Level;

public class SubCommandSetRotation extends SubCommand
{
    protected static final String help = "Change the rotation direction of a door";
    protected static final String argsHelp = "<doorUID/Name> <CLOCK || COUNTER || ANY>";
    protected static final int minArgCount = 1;
    protected static final CommandData command = CommandData.SETROTATION;

    public SubCommandSetRotation(final @NonNull BigDoorsSpigot plugin, final @NonNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }


    private void sendResultMessage(final @NonNull CommandSender sender, final @NonNull RotateDirection openDir)
    {
        plugin.getPLogger()
              .sendMessageToTarget(sender, Level.INFO,
                                   messages.getString(Message.COMMAND_SETROTATION_SUCCESS,
                                                      messages.getString(RotateDirection.getMessage(openDir))));
    }

    public void execute(final @NonNull CommandSender sender, final @NonNull AbstractDoorBase door,
                        final @NonNull RotateDirection openDir)
    {
        if (!(sender instanceof Player))
        {
            door.setOpenDir(openDir).syncData();
            sendResultMessage(sender, openDir);
            return;
        }

        if (!Util.hasPermissionForAction(((Player) sender).getUniqueId(), door, DoorAttribute.BLOCKS_TO_MOVE))
        {
            commandManager.handleException(new CommandActionNotAllowedException(), sender, null, null);
            return;
        }
        door.setOpenDir(openDir).syncData();
        sendResultMessage(sender, openDir);
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command cmd,
                             final @NonNull String label, final @NonNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException,
               CommandActionNotAllowedException, CommandPlayerNotFoundException
    {
        Player player = (Player) sender;
        Optional<ToolUser> toolUser = BigDoors.get().getToolUserManager().getToolUser(player.getUniqueId());
        if (toolUser.isPresent())
            toolUser.get().handleInput(args[1]);
        else
            // TODO: Specialized string.
            player.sendMessage(messages.getString(Message.ERROR_COMMAND_NOTHINGTOCONFIRM));
        return true;

//        Optional<WaitForCommand> commandWaiter = commandManager.isCommandWaiter(sender, getName());
//        if (commandWaiter.isPresent())
//            return commandManager.commandWaiterExecute(commandWaiter.get(), args, minArgCount);
//
//        RotateDirection openDir = RotateDirection.valueOf(args[2].toUpperCase());
//        commandManager.getDoorFromArg(sender, args[1], cmd, args).whenComplete(
//            (optionalDoorBase, throwable) -> optionalDoorBase.ifPresent(door -> execute(sender, door, openDir)));
//        return true;
    }
}
