package nl.pim16aap2.bigdoors.spigot.util.implementations.messageable;

import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.spigot.util.SpigotUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Level;

/**
 * Implementation of {@link IMessagingInterface} for Spigot.
 *
 * @author Pim
 * @see IMessagingInterface
 */
@Singleton
public class MessagingInterfaceSpigot implements IMessagingInterface
{
    private final String formattedName;

    @Inject
    public MessagingInterfaceSpigot(JavaPlugin plugin)
    {
        formattedName = IPLogger.formatName(plugin.getName());
    }

    @Override
    public void writeToConsole(Level level, String message)
    {
        Bukkit.getLogger().log(level, formattedName + message);
    }

    @Override
    public void messagePlayer(IPPlayer player, String message)
    {
        final @Nullable Player bukkitPlayer = Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer == null)
            return;
        SpigotUtil.messagePlayer(bukkitPlayer, message);
    }

    @Override
    public void sendMessageToTarget(Object target, Level level, String message)
    {
        if (target instanceof Player)
            SpigotUtil.messagePlayer((Player) target, message);
        else
            writeToConsole(level, ChatColor.stripColor(message));
    }

    @Override
    public void broadcastMessage(String message)
    {
        Bukkit.broadcastMessage(message);
    }
}
