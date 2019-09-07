package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;
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
    protected static final int minArgCount = 3;
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

    public void execute(final @NotNull CommandSender sender, final @NotNull DoorBase door,
                        final @NotNull RotateDirection openDir)
    {
        if (!(sender instanceof Player))
        {
            plugin.getDatabaseManager().updateDoorOpenDirection(door.getDoorUID(), openDir);
            sendResultMessage(sender, openDir);
            return;
        }

        final Player player = (Player) sender;
        plugin.getDatabaseManager()
              .hasPermissionForAction(player, door.getDoorUID(), DoorAttribute.BLOCKSTOMOVE).whenComplete(
            (isAllowed, throwable) ->
            {
                if (!isAllowed)
                {
                    commandManager.handleException(new CommandActionNotAllowedException(), sender, null, null);
                    return;
                }
                plugin.getDatabaseManager().updateDoorOpenDirection(door.getDoorUID(), openDir);
                sendResultMessage(sender, openDir);
            });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, IllegalArgumentException,
               CommandActionNotAllowedException, CommandPlayerNotFoundException
    {

        Optional<WaitForCommand> commandWaiter = commandManager.isCommandWaiter(sender, getName());
        if (commandWaiter.isPresent())
            return commandManager.commandWaiterExecute(commandWaiter.get(), args, minArgCount);

        RotateDirection openDir = RotateDirection.valueOf(args[2].toUpperCase());
        commandManager.getDoorFromArg(sender, args[1], cmd, args).whenComplete(
            (optionalDoorBase, throwable) -> optionalDoorBase.ifPresent(door -> execute(sender, door, openDir)));
        return true;
    }
}
