package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.commands.IPServer;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Represents the Spigot implementation of {@link IPServer}.
 *
 * @author Pim
 */
public class PServer implements IPServer
{
    private final @NotNull String pluginName;

    public PServer(final @NotNull JavaPlugin plugin)
    {
        pluginName = IPLogger.formatName(plugin.getName());
    }

    @Override
    public void sendMessage(@NotNull Level level, @NotNull String message)
    {
        Bukkit.getLogger().log(level, pluginName + message);
    }

    @Override
    public void sendMessage(@NotNull String message)
    {
        IPServer.super.sendMessage(message);
    }
}
