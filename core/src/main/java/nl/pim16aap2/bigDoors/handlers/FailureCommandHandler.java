/**
 *
 */
package nl.pim16aap2.bigDoors.handlers;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nl.pim16aap2.bigDoors.BigDoors;

/**
 *
 * @author Pim
 */
public class FailureCommandHandler implements CommandExecutor
{
    final String error;

    public FailureCommandHandler(final String error)
    {
        this.error = error;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        BigDoors.get().getMyLogger().logMessageToConsoleOnly(error);

        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            // Why .version? See LoginMessageHandler.
            player.sendMessage(ChatColor.YELLOW + ((player.isOp() || player.hasPermission("bigdoors.admin.version")) ?
                error : "An error occurred, please contact a server admin!"));
        }

        return true;
    }

    public String getError()
    {
        return error;
    }
}
