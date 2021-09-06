package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Singleton;
import java.util.logging.Level;

/**
 * Represents the Spigot implementation of {@link IPServer}.
 *
 * @author Pim
 */
@Singleton
public class PServer implements IPServer
{
    private final String pluginName;

    public PServer(JavaPlugin plugin)
    {
        pluginName = IPLogger.formatName(plugin.getName());
    }

    @Override
    public void sendMessage(Level level, String message)
    {
        Bukkit.getLogger().log(level, pluginName + message);
    }

    @Override
    public void sendMessage(String message)
    {
        IPServer.super.sendMessage(message);
    }
}
