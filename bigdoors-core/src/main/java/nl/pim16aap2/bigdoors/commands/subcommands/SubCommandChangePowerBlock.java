package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.exceptions.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.DoorAttribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SubCommandChangePowerBlock extends SubCommand
{
    protected static final String help = "Change the location of the powerblock of a door.";
    protected static final String argsHelp = "<doorUID/Name>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.CHANGEPOWERBLOCK;

    public SubCommandChangePowerBlock(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    public boolean execute(Player player, DoorBase door) throws CommandActionNotAllowedException
    {
        if (!plugin.getDatabaseManager().hasPermissionForAction(player, door.getDoorUID(),
                                                                DoorAttribute.RELOCATEPOWERBLOCK))
            throw new CommandActionNotAllowedException();
        plugin.getDatabaseManager().startPowerBlockRelocator(player, door);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
            throws CommandSenderNotPlayerException, CommandPermissionException, CommandActionNotAllowedException,
                   IllegalArgumentException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();
        return execute((Player) sender, commandManager.getDoorFromArg(sender, args[getMinArgCount() - 1]));
    }
}
