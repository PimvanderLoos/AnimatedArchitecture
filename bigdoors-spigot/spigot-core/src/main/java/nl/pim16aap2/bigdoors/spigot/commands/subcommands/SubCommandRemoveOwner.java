package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotAdapter;
import nl.pim16aap2.bigdoors.spigot.waitforcommand.WaitForCommand;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class SubCommandRemoveOwner extends SubCommand
{
    protected static final String help = "Remove another owner for a door.";
    protected static final String helpArgs = "{door} <player>";
    protected static final int minArgCount = 3;
    protected static final CommandData command = CommandData.REMOVEOWNER;
    private int actualMinArgCount;

    public SubCommandRemoveOwner(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
        actualMinArgCount = getMinArgCount();
    }

    public boolean execute(final @NotNull CommandSender sender, final @NotNull AbstractDoorBase door,
                           final @NotNull String playerArg)
        throws CommandPlayerNotFoundException
    {
        UUID playerUUID = CommandManager.getPlayerFromArg(playerArg);
        IPPlayer player = SpigotAdapter.wrapPlayer(Bukkit.getOfflinePlayer(playerUUID));
        return execute(sender, door.getDoorUID(), player);
    }

    private boolean execute(final @NotNull CommandSender sender, final long doorUID, final @NotNull IPPlayer target)
    {
        BigDoors.get().getDatabaseManager().getDoor(target, doorUID).whenComplete(
            (optionalDoor, throwable) ->
                optionalDoor.ifPresent(
                    door ->
                    {
                        boolean hasPermission = true;
                        if (sender instanceof Player)
                        {
                            IPPlayer player = SpigotAdapter.wrapPlayer((Player) sender);
                            try
                            {
                                hasPermission = BigDoors.get().getDatabaseManager()
                                                        .hasPermissionForAction(player, door.getDoorUID(),
                                                                                DoorAttribute.REMOVEOWNER).get();
                            }
                            catch (InterruptedException | ExecutionException e)
                            {
                                plugin.getPLogger().logException(e);
                                hasPermission = false;
                            }
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
                                                        .get();
                        }
                        catch (InterruptedException | ExecutionException e)
                        {
                            plugin.getPLogger().logException(e);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, final @NotNull String[] args)
        throws IllegalArgumentException, CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        Optional<WaitForCommand> commandWaiter = commandManager.isCommandWaiter(sender, getName());
        if (commandWaiter.isPresent())
            return commandManager.commandWaiterExecute(commandWaiter.get(), args, minArgCount);

        UUID playerUUID = CommandManager.getPlayerFromArg(args[getMinArgCount() - 1]);
        IPPlayer player = SpigotAdapter.wrapPlayer(Bukkit.getOfflinePlayer(playerUUID));

        commandManager.getDoorFromArg(sender, args[getMinArgCount() - 2], cmd, args).whenComplete(
            (optionalDoor, throwable) ->
                optionalDoor.ifPresent(door -> execute(sender, door.getDoorUID(), player)));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public String getHelp(@NotNull CommandSender sender)
    {
        return help;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHelpArguments()
    {
        return helpArgs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinArgCount()
    {
        return minArgCount;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public CommandData getCommandData()
    {
        return command;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public String getPermission()
    {
        return CommandData.getPermission(command);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    @Override
    public String getName()
    {
        return CommandData.getCommandName(command);
    }
}
