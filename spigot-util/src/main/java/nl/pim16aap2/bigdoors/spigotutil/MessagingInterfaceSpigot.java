package nl.pim16aap2.bigdoors.spigotutil;

import java.util.UUID;
import java.util.logging.Level;

import nl.pim16aap2.bigdoors.util.PLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import nl.pim16aap2.bigdoors.util.IMessagingInterface;

/**
 * Implementation of {@link IMessagingInterface} for Spigot.
 * @see IMessagingInterface
 *
 * @author Pim
 */
public class MessagingInterfaceSpigot implements IMessagingInterface
{
    private final String formattedName;

    public MessagingInterfaceSpigot(JavaPlugin plugin)
    {
        formattedName = PLogger.formatName(plugin.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToConsole(Level level, String message)
    {
        Bukkit.getLogger().log(level, formattedName + message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messagePlayer(UUID playerUUID, String message)
    {
        SpigotUtil.messagePlayer(Bukkit.getPlayer(playerUUID), message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessageToTarget(final Object target, final Level level, final String str)
    {
        if (target instanceof Player)
            SpigotUtil.messagePlayer((Player) target, str);
        else
            writeToConsole(level, ChatColor.stripColor(str));
    }
}
