package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.waitforcommand.WaitForCommand;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
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

    public boolean execute(final @NotNull CommandSender sender, final @NotNull AbstractDoorBase door,
                           final @NotNull String playerArg, final int permission)
        throws CommandPlayerNotFoundException, ExecutionException, InterruptedException
    {
        throw new UnsupportedOperationException();
//        final UUID playerUUID = CommandManager.getPlayerFromArg(playerArg);
//        final IPPlayer player = SpigotAdapter.wrapPlayer(Bukkit.getOfflinePlayer(playerUUID));
//
//        // No need to check for permissions if the sender wasn't a player.
//        if (!(sender instanceof Player))
//        {
//            boolean success = BigDoors.get().getDatabaseManager().addOwner(door, player, permission).get();
//            plugin.getPLogger().sendMessageToTarget(sender, Level.INFO,
//                                                    success ?
//                                                    messages.getString(Message.COMMAND_ADDOWNER_SUCCESS) :
//                                                    messages.getString(Message.COMMAND_ADDOWNER_FAIL));
//            return true;
//        }
//
//        if (!Util.hasPermissionForAction(player, door, DoorAttribute.ADDOWNER))
//        {
//            commandManager.handleException(new CommandActionNotAllowedException(), sender, null, null);
//            return true;
//        }
//        BigDoors.get().getDatabaseManager().addOwner(door, player, permission).whenComplete(
//            (result, throwable) ->
//                plugin.getPLogger()
//                      .sendMessageToTarget(sender, Level.INFO, result ?
//                                                               messages.getString(Message.COMMAND_ADDOWNER_SUCCESS) :
//                                                               messages.getString(Message.COMMAND_ADDOWNER_FAIL)));
//        return true;
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
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO,
                                       messages.getString(Message.ERROR_COMMAND_INVALIDPERMISSIONVALUE, args[pos]));
        }
        return permission;
    }

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
