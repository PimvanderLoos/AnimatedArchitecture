package nl.pim16aap2.bigdoors.spigot.util;

import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Implementation of {@link IMessagingInterface} for Spigot.
 *
 * @author Pim
 * @see IMessagingInterface
 */
public class MessagingInterfaceSpigot implements IMessagingInterface
{
    private final String formattedName;

    public MessagingInterfaceSpigot(final @NotNull JavaPlugin plugin)
    {
        formattedName = PLogger.formatName(plugin.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToConsole(final @NotNull Level level, final @NotNull String message)
    {
        Bukkit.getLogger().log(level, formattedName + message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messagePlayer(@NotNull IPPlayer player, @NotNull String message)
    {

        Player bukkitPlayer = Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer == null)
            return;
        SpigotUtil.messagePlayer(bukkitPlayer, message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessageToTarget(final @NotNull Object target, final @NotNull Level level,
                                    final @NotNull String message)
    {
        if (target instanceof Player)
            SpigotUtil.messagePlayer((Player) target, message);
        else
            writeToConsole(level, ChatColor.stripColor(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcastMessage(final @NotNull String message)
    {
        Bukkit.broadcastMessage(message);
    }
}
