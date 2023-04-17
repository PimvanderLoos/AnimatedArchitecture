package nl.pim16aap2.animatedarchitecture.spigot.core.listeners;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.util.Constants;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a command listener that replaces all command input with failure messages.
 * <p>
 * This is intended to be used as a failure state.
 *
 * @author Pim
 */
@Flogger
public final class BackupCommandListener implements CommandExecutor
{
    private final JavaPlugin plugin;
    private final String errorMessage;

    public BackupCommandListener(JavaPlugin plugin, @Nullable String errorMessage)
    {
        this.plugin = plugin;
        this.errorMessage = errorMessage == null ? "NULL" : errorMessage;
        registerCommands();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        log.atWarning().log("%s", errorMessage);

        if (sender instanceof Player player)
            player.sendMessage(ChatColor.YELLOW + getReturnMessage(player));

        return true;
    }

    private String getReturnMessage(Player player)
    {
        return isAdmin(player) ? errorMessage : "An error occurred, please contact a server admin!";
    }

    private boolean isAdmin(Player player)
    {
        return player.hasPermission(Constants.PERMISSION_PREFIX_ADMIN + "restart");
    }

    /**
     * Registers this listener for all commands defined in the plugin.yml file.
     */
    private void registerCommands()
    {
        plugin.getDescription().getCommands().keySet().forEach(this::registerCommand);
    }

    /**
     * Registers this listener for a command with the provided name.
     *
     * @param commandName
     *     The name of the command.
     */
    private void registerCommand(String commandName)
    {
        registerCommand(plugin.getCommand(commandName));
    }

    private void registerCommand(@Nullable PluginCommand command)
    {
        if (command == null)
            return;
        command.setExecutor(plugin);
    }
}
