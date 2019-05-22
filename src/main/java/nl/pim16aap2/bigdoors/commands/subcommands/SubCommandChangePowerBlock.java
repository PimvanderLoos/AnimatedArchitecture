package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.Door;
import nl.pim16aap2.bigdoors.commands.CommandActionNotAllowedException;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandManager;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.util.DoorAttribute;

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

    public boolean execute(Player player, Door door) throws CommandActionNotAllowedException
    {
        if (!plugin.getCommander().hasPermissionForAction(player, door.getDoorUID(), DoorAttribute.RELOCATEPOWERBLOCK))
            throw new CommandActionNotAllowedException();
        plugin.getCommander().startPowerBlockRelocator(player, door);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException, CommandActionNotAllowedException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        Player player = (Player) sender;
        return execute(player, plugin.getCommander().getDoor(args[0], player));
    }
}
