package nl.pim16aap2.bigdoors.spigot.commands.subcommands;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.exceptions.CommandPermissionException;
import nl.pim16aap2.bigdoors.exceptions.CommandSenderNotPlayerException;
import nl.pim16aap2.bigdoors.spigot.BigDoorsSpigot;
import nl.pim16aap2.bigdoors.spigot.commands.CommandData;
import nl.pim16aap2.bigdoors.spigot.managers.CommandManager;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

public class SubCommandConfirm extends SubCommand
{
    protected static final String help = "Confirms the current task (e.g. wait for command input)";
    protected static final String argsHelp = null;
    protected static final int minArgCount = 1;
    protected static final CommandData command = CommandData.CONFIRM;

    public SubCommandConfirm(final @NonNull BigDoorsSpigot plugin, final @NonNull CommandManager commandManager)
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
        Optional<ToolUser> toolUser = BigDoors.get().getToolUserManager().getToolUser(player.getUniqueId());
        if (toolUser.isPresent())
            toolUser.get().handleInput(true);
        else
            player.sendMessage(messages.getString(Message.ERROR_COMMAND_NOTHINGTOCONFIRM));
        return true;
    }
}
