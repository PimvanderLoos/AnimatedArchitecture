package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPlayerNotFoundException;
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

public class SubCommandRemoveOwner extends SubCommand
{
    protected static final String help = "Remove another owner for a door.";
    protected static final String helpArgs = "{door} <player>";
    protected static final int minArgCount = 3;
    protected static final CommandData command = CommandData.REMOVEOWNER;
    private int actualMinArgCount;

    public SubCommandRemoveOwner(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
        actualMinArgCount = getMinArgCount();
    }

    public boolean execute(CommandSender sender, DoorBase door, String playerArg)
        throws CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        UUID playerUUID = CommandManager.getPlayerFromArg(playerArg);

        if (sender instanceof Player && !plugin.getDatabaseManager()
                                               .hasPermissionForAction((Player) sender, door.getDoorUID(),
                                                                       DoorAttribute.REMOVEOWNER))
            throw new CommandActionNotAllowedException();

        if (plugin.getDatabaseManager().removeOwner(door, playerUUID))
        {
            plugin.getPLogger()
                  .sendMessageToTarget(sender, Level.INFO, messages.getString(Message.COMMAND_REMOVEOWNER_SUCCESS));
            return true;
        }
        plugin.getPLogger()
              .sendMessageToTarget(sender, Level.INFO, messages.getString(Message.COMMAND_REMOVEOWNER_FAIL, playerArg));
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                             @NotNull String[] args)
        throws IllegalArgumentException, CommandPlayerNotFoundException, CommandActionNotAllowedException
    {
        Optional<WaitForCommand> commandWaiter = commandManager.isCommandWaiter(sender, getName());
        if (commandWaiter.isPresent())
            return commandManager.commandWaiterExecute(commandWaiter.get(), args, minArgCount);
        return execute(sender, commandManager.getDoorFromArg(sender, args[getMinArgCount() - 2]),
                       args[getMinArgCount() - 1]);
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
