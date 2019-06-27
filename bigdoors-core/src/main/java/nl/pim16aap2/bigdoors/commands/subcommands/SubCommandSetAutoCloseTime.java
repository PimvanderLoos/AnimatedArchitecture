package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.*;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigotutil.DoorAttribute;
import nl.pim16aap2.bigdoors.waitforcommand.WaitForCommand;

public class SubCommandSetAutoCloseTime extends SubCommand
{
    protected static final String help = "Changes/Sets time (in s) for doors to automatically close. -1 to disable.";
    protected static final String argsHelp = "{door} <time>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.SETAUTOCLOSETIME;

    public SubCommandSetAutoCloseTime(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(CommandSender sender, DoorBase door, String timeArg)
        throws CommandActionNotAllowedException, CommandInvalidVariableException
    {
        if (sender instanceof Player && plugin.getDatabaseManager().hasPermissionForAction((Player) sender, door.getDoorUID(), DoorAttribute.CHANGETIMER))
            throw new CommandActionNotAllowedException();

        int time = CommandManager.getIntegerFromArg(timeArg);

        plugin.getDatabaseManager().setDoorOpenTime(door.getDoorUID(), time);
        if (time != -1)
            plugin.getMyLogger().sendMessageToTarget(sender, null,
                                                plugin.getMessages().getString("COMMAND.SetTime.Success") + time + "s.");
        else
            plugin.getMyLogger().sendMessageToTarget(sender, null,
                                                plugin.getMessages().getString("COMMAND.SetTime.Disabled"));
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandPlayerNotFoundException,
        CommandActionNotAllowedException, CommandInvalidVariableException
    {
        if (sender instanceof Player)
        {
            WaitForCommand cw = plugin.isCommandWaiter((Player) sender);
            if (cw != null && cw.getCommand().equals(getName()))
            {
                if (args.length == minArgCount)
                    return cw.executeCommand(args);
                cw.abortSilently();
            }
        }
        return execute(sender, commandManager.getDoorFromArg(sender, args[1]), args[2]);
    }
}
