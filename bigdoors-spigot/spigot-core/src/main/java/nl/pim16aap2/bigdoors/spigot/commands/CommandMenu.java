package nl.pim16aap2.bigdoors.spigot.commands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandMenu;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class CommandMenu extends SubCommandMenu
{
    protected static final String name = "bdm";
    private static final CommandData command = CommandData.BDM;
    private static final int minArgCount = 0;

    public CommandMenu(final @NonNull BigDoorsSpigot plugin, final @NonNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command cmd,
                             final @NonNull String label, @NonNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        return super.onCommand(sender, cmd, label, args);
    }

    @Override
    public @NonNull String getHelp(final @NonNull CommandSender sender)
    {
        return SpigotUtil.helpFormat(name, super.getHelp(sender));
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
