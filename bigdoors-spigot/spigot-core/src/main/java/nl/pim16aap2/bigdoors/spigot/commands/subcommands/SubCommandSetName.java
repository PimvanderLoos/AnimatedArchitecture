package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SubCommandSetName extends SubCommand
{
    protected static final String help = "Set the name of the door in the door creation process.";
    protected static final String argsHelp = "<doorName>";
    protected static final int minArgCount = 2;
    protected static final CommandData command = CommandData.SETNAME;

    public SubCommandSetName(final @NonNull BigDoorsSpigot plugin, final @NonNull CommandManager commandManager)
    {
        super(plugin, commandManager);
        init(help, argsHelp, minArgCount, command);
    }

    @Override
    public boolean onCommand(final @NonNull CommandSender sender, final @NonNull Command cmd,
                             final @NonNull String label, final @NonNull String[] args)
        throws CommandSenderNotPlayerException, CommandPermissionException
    {
        if (!(sender instanceof Player))
            throw new CommandSenderNotPlayerException();

        Player player = (Player) sender;
        Optional<ToolUser> tu = BigDoors.get().getToolUserManager().getToolUser(player.getUniqueId());
        if (tu.isPresent() && tu.get() instanceof Creator)
        {
            if (args.length == getMinArgCount() && Util.isValidDoorName(args[getMinArgCount() - 1]))
            {
                tu.get().handleInput(args[getMinArgCount() - 1]);
                return true;
            }
            return false;
        }
        SpigotUtil.messagePlayer(player, messages.getString(Message.ERROR_PLAYERISNOTBUSY));
        return true;
    }
}
