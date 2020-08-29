package nl.pim16aap2.bigdoors.spigot.commands;

import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.subcommands.SubCommandMenu;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CommandMenu extends SubCommandMenu
{
    protected static final String name = "bdm";
    private static final CommandData command = CommandData.BDM;
    private static final int minArgCount = 0;

    public CommandMenu(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    @Override
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd,
                             final @NotNull String label, @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        return super.onCommand(sender, cmd, label, args);
    }

    @Override
    public @NotNull String getHelp(final @NotNull CommandSender sender)
    {
        return SpigotUtil.helpFormat(name, super.getHelp(sender));
    }

    @Override
    public @NotNull CommandData getCommandData()
    {
        return command;
    }

    @Override
    public @NotNull String getPermission()
    {
        return CommandData.getPermission(command);
    }

    @Override
    public @NotNull String getName()
    {
        return CommandData.getCommandName(command);
    }
}
