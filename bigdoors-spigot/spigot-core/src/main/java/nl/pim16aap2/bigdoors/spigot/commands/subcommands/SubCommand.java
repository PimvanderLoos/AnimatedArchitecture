package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.commands.ICommand;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SubCommand implements ICommand
{
    protected final BigDoorsSpigot plugin;
    protected final CommandManager commandManager;
    protected final Messages messages;

    protected String help;
    @Nullable
    protected String argsHelp;
    protected int minArgCount;
    protected CommandData command;

    public SubCommand(final @NotNull BigDoorsSpigot plugin, final @NotNull CommandManager commandManager)
    {
        this.plugin = plugin;
        messages = plugin.getMessages();
        this.commandManager = commandManager;
    }

    protected final void init(final @NotNull String help, final @Nullable String argsHelp, final int minArgCount,
                              final @NotNull CommandData command)
    {
        this.help = help;
        this.argsHelp = argsHelp;
        this.minArgCount = minArgCount;
        this.command = command;
    }

    @Override
    @NotNull
    public String getHelp(final @NotNull CommandSender sender)
    {
        return help;
    }

    /**
     * Gets the help information of the arguments of this command.
     *
     * @return The help information of the arguments of this command.
     */
    public @Nullable String getHelpArguments()
    {
        return argsHelp;
    }

    @Override
    public int getMinArgCount()
    {
        return commandManager.getCommand(CommandData.getSuperCommand(command)).getMinArgCount() + minArgCount;
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
