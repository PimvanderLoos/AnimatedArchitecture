package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.waitforcommand.WaitForCommand;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class SubCommandRemoveOwner extends SubCommand
{
    protected static final String help = "Remove another owner for a door.";
    protected static final String helpArgs = "{door} <player>";
    protected static final int minArgCount = 3;
    protected static final CommandData command = CommandData.REMOVEOWNER;
    private int actualMinArgCount;

    public SubCommandRemoveOwner(final @NonNull BigDoorsSpigot plugin, final @NonNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
        actualMinArgCount = getMinArgCount();
    }

    public boolean execute(final @NonNull CommandSender sender, final @NonNull AbstractDoorBase door,
                           final @NonNull String playerArg)
        throws CommandPlayerNotFoundException
    {
//        UUID playerUUID = CommandManager.getPlayerFromArg(playerArg);
//        IPPlayer player = SpigotAdapter.wrapPlayer(Bukkit.getOfflinePlayer(playerUUID));
//        return execute(sender, door.getDoorUID(), player);
        throw new UnsupportedOperationException();
    }

    private boolean execute(final @NonNull CommandSender sender, final long doorUID, final @NonNull IPPlayer target)
    {
        BigDoors.get().getDatabaseManager().getDoor(target, doorUID).whenComplete(
            (optionalDoor, throwable) ->
                optionalDoor.ifPresent(
                    door ->
                    {
                        final boolean hasPermission;
                        if (!(sender instanceof Player))
                            hasPermission = true;
                        else
                        {
                            final @NonNull Player player = (Player) sender;
                            final int executorPermission = door.getDoorOwner(player.getUniqueId())
                                                               .map(DoorOwner::getPermission).orElse(Integer.MAX_VALUE);
                            final int targetPermission = door.getDoorOwner(target.getUUID())
                                                             .map(DoorOwner::getPermission).orElse(Integer.MAX_VALUE);

                            if (executorPermission == Integer.MAX_VALUE || targetPermission == Integer.MAX_VALUE)
                            {
                                commandManager
                                    .handleException(new CommandPermissionException(), sender, null, null);
                                return;
                            }

                            hasPermission = executorPermission < targetPermission &&
                                Util.hasPermissionForAction(((Player) sender).getUniqueId(), door,
                                                            DoorAttribute.REMOVE_OWNER);
                        }

                        if (!hasPermission)
                        {
                            commandManager.handleException(new CommandActionNotAllowedException(), sender, null, null);
                            return;
                        }

                        boolean successfulRemoval;
                        try
                        {
                            // TODO: Make sure this doesn't run on the main thread. I don't think it will, but
                            //       it's good to check.
                            successfulRemoval = BigDoors.get().getDatabaseManager().removeOwner(door, target.getUUID())
                                                        .get() == DatabaseManager.ActionResult.SUCCESS;
                        }
                        catch (InterruptedException e)
                        {
                            BigDoors.get().getPLogger().logThrowable(e);
                            successfulRemoval = false;
                            Thread.currentThread().interrupt();
                        }
                        catch (ExecutionException e)
                        {
                            plugin.getPLogger().logThrowable(e);
                            successfulRemoval = false;
                        }
                        if (successfulRemoval)
                            plugin.getPLogger()
                                  .sendMessageToTarget(sender, Level.INFO,
                                                       messages.getString(Message.COMMAND_REMOVEOWNER_SUCCESS));
                        else
                            plugin.getPLogger()
                                  .sendMessageToTarget(sender, Level.INFO,
                                                       messages.getString(Message.COMMAND_REMOVEOWNER_FAIL,
                                                                          target.getUUID().toString()));
                    }
                )
        );
        return true;
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command cmd,
                             final @NonNull String label, final @NonNull String[] args)
        throws IllegalArgumentException, CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        Optional<WaitForCommand> commandWaiter = commandManager.isCommandWaiter(sender, getName());
        if (commandWaiter.isPresent())
            return commandManager.commandWaiterExecute(commandWaiter.get(), args, minArgCount);

        throw new UnsupportedOperationException();
//        UUID playerUUID = CommandManager.getPlayerFromArg(args[getMinArgCount() - 1]);
//        IPPlayer player = SpigotAdapter.wrapPlayer(Bukkit.getOfflinePlayer(playerUUID));
//
//        commandManager.getDoorFromArg(sender, args[getMinArgCount() - 2], cmd, args).whenComplete(
//            (optionalDoor, throwable) ->
//                optionalDoor.ifPresent(door -> execute(sender, door.getDoorUID(), player)));
//        return true;
    }

    @Override
    public @NonNull String getHelp(@NonNull CommandSender sender)
    {
        return help;
    }

    @Override
    public String getHelpArguments()
    {
        return helpArgs;
    }

    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }

    @Override
    public @NonNull CommandData getCommandData()
    {
        return command;
    }

    @Override
    public @NonNull String getPermission()
    {
        return CommandData.getPermission(command);
    }

    @Override
    public @NonNull String getName()
    {
        return CommandData.getCommandName(command);
    }
}
