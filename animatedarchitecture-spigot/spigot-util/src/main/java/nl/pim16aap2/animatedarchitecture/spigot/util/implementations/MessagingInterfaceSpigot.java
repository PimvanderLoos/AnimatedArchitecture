package nl.pim16aap2.animatedarchitecture.spigot.util.implementations;

import nl.pim16aap2.animatedarchitecture.core.api.IMessagingInterface;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.spigot.util.SpigotUtil;
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
 * @see IMessagingInterface
 */
@Singleton
public class MessagingInterfaceSpigot implements IMessagingInterface
{
    private final String formattedName;

    @Inject
    public MessagingInterfaceSpigot(JavaPlugin plugin)
    {
        formattedName = '[' + plugin.getName() + "] ";
    }

    @Override
    public void writeToConsole(Level level, String message)
    {
        Bukkit.getLogger().log(level, formattedName + message);
    }

    @Override
    public void messagePlayer(IPlayer player, String message)
    {
        final @Nullable Player bukkitPlayer = Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer == null)
            return;
        SpigotUtil.messagePlayer(bukkitPlayer, message);
    }

    @Override
    public void sendMessageToTarget(Object target, Level level, String message)
    {
        if (target instanceof Player player)
            SpigotUtil.messagePlayer(player, message);
        else
            writeToConsole(level, ChatColor.stripColor(message));
    }

    @Override
    public void broadcastMessage(String message)
    {
        Bukkit.broadcastMessage(message);
    }
}
