package nl.pim16aap2.bigdoors.commands.subcommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.commands.CommandPermissionException;
import nl.pim16aap2.bigdoors.commands.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.gui.GUI;
import nl.pim16aap2.bigdoors.managers.CommandManager;

public class SubCommandMenu extends SubCommand
{
    protected static final String help = "Opens BigDoors' GUI.";
    protected static final String argsHelp = null;
    protected static final int minArgCount = 0;
    protected static final CommandData command = CommandData.MENU;

    public SubCommandMenu(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();
        plugin.addGUIUser(new GUI(plugin, (Player) sender));
        return true;
    }
}
