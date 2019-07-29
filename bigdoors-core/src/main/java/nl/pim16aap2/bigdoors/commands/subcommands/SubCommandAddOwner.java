package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class SubCommandAddOwner extends SubCommand
{
    protected final String help = "Add another owner for a door.";
    protected final String argsHelp = "{doorUID/Name} <player> [permissionLevel]";
    protected final int minArgCount = 3;
    protected CommandData command = CommandData.ADDOWNER;

    public SubCommandAddOwner(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, DoorBase door, String playerArg, int permission)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        UUID playerUUID = CommandManager.getPlayerFromArg(playerArg);

        if (sender instanceof Player && !plugin.getDatabaseManager()
                                               .hasPermissionForAction((Player) sender, door.getDoorUID(),
                                                                       DoorAttribute.ADDOWNER))
            throw new CommandActionNotAllowedException();

        if (plugin.getDatabaseManager().addOwner(door, playerUUID, permission))
        {
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO, messages.getString(Message.COMMAND_ADDOWNER_SUCCESS));
            return true;
        }
        plugin.getPLogger()
              .sendMessageToTarget(sender, Level.INFO, messages.getString(Message.COMMAND_ADDOWNER_FAIL));
        return false;

    }

    public int getPermissionFromArgs(CommandSender sender, String[] args, int pos)
    {
        int permission = Integer.MAX_VALUE;
        try
        {
            permission = CommandManager.getIntegerFromArg(args[pos]);
        }
        catch (Exception e)
        {
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO,
                                                    messages.getString(Message.ERROR_COMMAND_INVALIDPERMISSIONVALUE,
                                                                       args[pos]));
        }
        return permission;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                             @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException,
               CommandActionNotAllowedException, IllegalArgumentException
    {
        Optional<WaitForCommand> commandWaiter = commandManager.isCommandWaiter(sender, getName());
        if (commandWaiter.isPresent())
            return commandManager.commandWaiterExecute(commandWaiter.get(), args, minArgCount);
        return execute(sender, commandManager.getDoorFromArg(sender, args[1]), args[2],
                       getPermissionFromArgs(sender, args, 3));
    }
}
