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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
            throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        Player player = (Player) sender;
        ToolUser tu = plugin.getToolUser(player);
        if (tu instanceof Creator)
        {
            if (args.length == getMinArgCount() && Util.isValidDoorName(args[getMinArgCount() - 1]))
            {
                ((Creator) tu).setName(args[getMinArgCount() - 1]);
                return true;
            }
            return false;
        }
        SpigotUtil.messagePlayer(player, plugin.getMessages().getString("GENERAL.NotBusy"));
        return true;
    }
}
