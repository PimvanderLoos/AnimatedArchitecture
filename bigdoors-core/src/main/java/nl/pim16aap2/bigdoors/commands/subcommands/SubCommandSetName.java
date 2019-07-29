package nl.pim16aap2.bigdoors.commands.subcommands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.commands.CommandData;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigotutil.SpigotUtil;
import nl.pim16aap2.bigdoors.toolusers.Creator;
import nl.pim16aap2.bigdoors.toolusers.ToolUser;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SubCommandSetName extends SubCommand
{
    protected static final String help = "Set the name of the door in the door creation process.";
    protected static final String argsHelp = "<doorName>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.SETNAME;

    public SubCommandSetName(final BigDoors plugin, final CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
                             @NotNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        Player player = (Player) sender;
        Optional<ToolUser> tu = plugin.getToolUser(player);
        if (tu.isPresent() && tu.get() instanceof Creator)
        {
            if (args.length == getMinArgCount() && Util.isValidDoorName(args[getMinArgCount() - 1]))
            {
                ((Creator) tu.get()).setName(args[getMinArgCount() - 1]);
                return true;
            }
            return false;
        }
        SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_PLAYERISNOTBUSY));
        return true;
    }
}
