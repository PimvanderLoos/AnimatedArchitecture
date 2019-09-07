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

    public SubCommandAddOwner(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(final @NotNull CommandSender sender, final @NotNull DoorBase door,
                           final @NotNull String playerArg, final int permission)
        throws CommandPlayerNotFoundException
    {
        final UUID playerUUID = CommandManager.getPlayerFromArg(playerArg);

        // No need to check for permissions if the sender wasn't a player.
        if (!(sender instanceof Player))
        {
            boolean success = plugin.getDatabaseManager().addOwner(door, playerUUID, permission);
            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO,
                                                    success ?
                                                    messages.getString(Message.COMMAND_ADDOWNER_SUCCESS) :
                                                    messages.getString(Message.COMMAND_ADDOWNER_FAIL));
            return true;
        }

        plugin.getDatabaseManager().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.ADDOWNER)
              .whenComplete(
                  (isAllowed, throwable) ->
                  {
                      if (!isAllowed)
                      {
                          commandManager.handleException(new CommandActionNotAllowedException(), sender, null, null);
                          return;
                      }
                      boolean success = plugin.getDatabaseManager().addOwner(door, playerUUID, permission);
                      plugin.getPLogger().sendMessageToTarget(sender, Level.INFO,
                                                              success ?
                                                              messages.getString(Message.COMMAND_ADDOWNER_SUCCESS) :
                                                              messages.getString(Message.COMMAND_ADDOWNER_FAIL));
                  });
        return true;
    }

    public int getPermissionFromArgs(final @NotNull CommandSender sender, final @NotNull String[] args, final int pos)
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
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException,
               CommandActionNotAllowedException, IllegalArgumentException
    {
        Optional<WaitForCommand> commandWaiter = commandManager.isCommandWaiter(sender, getName());
        if (commandWaiter.isPresent())
            return commandManager.commandWaiterExecute(commandWaiter.get(), args, minArgCount);

        commandManager.getDoorFromArg(sender, args[1], cmd, args).whenComplete(
            (optionalDoor, throwable) ->
                optionalDoor.ifPresent(
                    door ->
                    {
                        try
                        {
                            execute(sender, door, args[2], getPermissionFromArgs(sender, args, 3));
                        }
                        catch (Exception e)
                        {
                            commandManager.handleException(e, sender, cmd, args);
                        }
                    }));
        return true;
    }
}
