package nl.pim16aap2.bigdoors.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.subcommands.SubCommandMenu;
import nl.pim16aap2.bigdoors.util.Util;

public class CommandMenu extends SubCommandMenu
{
    protected static final String name = "bdm";

    public CommandMenu(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        return super.onCommand(sender, cmd, label, args);
    }

    @Override
    public String getHelp(CommandSender sender)
    {
        return Util.helpFormat(name, super.getHelp(sender));
    }

    @Override
    public String getPermission()
    {
        return super.getPermission();
    }

    @Override
    public String getName()
    {
        return name;
    }
}
