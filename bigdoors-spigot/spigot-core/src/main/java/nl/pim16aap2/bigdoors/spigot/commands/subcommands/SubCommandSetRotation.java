package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Level;

public class SubCommandSetRotation extends SubCommand
{
    protected static final String help = "Change the rotation direction of a door";
    protected static final String argsHelp = "<doorUID/Name> <CLOCK || COUNTER || ANY>";
    protected static final int minArgCount = 1;
    protected static final CommandData command = CommandData.SETROTATION;

    public SubCommandSetRotation(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }


    private void sendResultMessage(final @NotNull CommandSender sender, final @NotNull RotateDirection openDir)
    {
        plugin.getPLogger()
              .sendMessageToTarget(sender, Level.INFO,
                                   messages.getString(Message.COMMAND_SETROTATION_SUCCESS,
                                                      messages.getString(RotateDirection.getMessage(openDir))));
    }

    public void execute(final @NotNull CommandSender sender, final @NotNull AbstractDoorBase door,
                        final @NotNull RotateDirection openDir)
    {
        if (!(sender instanceof Player))
        {
            BigDoors.get().getDatabaseManager().updateDoorOpenDirection(door.getDoorUID(), openDir);
            sendResultMessage(sender, openDir);
            return;
        }


        final IPPlayer player = SpigotAdapter.wrapPlayer((Player) sender);
        BigDoors.get().getDatabaseManager()
                .hasPermissionForAction(player, door.getDoorUID(), DoorAttribute.BLOCKSTOMOVE).whenComplete(
            (isAllowed, throwable) ->
            {
                if (!isAllowed)
                {
                    commandManager.handleException(new CommandActionNotAllowedException(), sender, null, null);
                    return;
                }
                BigDoors.get().getDatabaseManager().updateDoorOpenDirection(door.getDoorUID(), openDir);
                sendResultMessage(sender, openDir);
            });
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException,
               CommandActionNotAllowedException, CommandPlayerNotFoundException
    {
        Player player = (Player) sender;
        Optional<ToolUser> toolUser = ToolUserManager.get().getToolUser(player.getUniqueId());
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
