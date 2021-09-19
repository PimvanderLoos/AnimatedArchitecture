package nl.pim16aap2.bigdoors.spigot.util.implementations.pserver;

import nl.pim16aap2.bigdoors.commands.IPServer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.inject.Inject;
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
    private final String formattedName;

    @Inject
    public PServer(JavaPlugin plugin)
    {
        formattedName = '[' + plugin.getName() + "] ";
    }

    @Override
    public void sendMessage(Level level, String message)
    {
        Bukkit.getLogger().log(level, formattedName + message);
    }

    @Override
    public void sendMessage(String message)
    {
        IPServer.super.sendMessage(message);
    }
}
