package nl.pim16aap2.bigdoors.spigot.util.implementations;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Represents the Spigot implementation of {@link IPServer}.
 *
 * @author Pim
 */
public class PServer implements IPServer
{
    private final @NonNull String pluginName;

    public PServer(final @NonNull JavaPlugin plugin)
    {
        pluginName = IPLogger.formatName(plugin.getName());
    }

    @Override
    public void sendMessage(@NonNull Level level, @NonNull String message)
    {
        Bukkit.getLogger().log(level, pluginName + message);
    }

    @Override
    public void sendMessage(@NonNull String message)
    {
        IPServer.super.sendMessage(message);
    }
}
