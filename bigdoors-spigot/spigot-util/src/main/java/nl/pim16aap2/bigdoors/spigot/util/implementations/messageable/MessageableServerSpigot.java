package nl.pim16aap2.bigdoors.spigot.util.implementations.messageable;

import nl.pim16aap2.bigdoors.api.IMessageable;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.bukkit.ChatColor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.logging.Level;

/**
 * Represents the server as an {@link IMessageable}.
 *
 * @author Pim
 */
@Singleton
public final class MessageableServerSpigot implements IMessageable
{
    private final IPLogger logger;

    @Inject
    public MessageableServerSpigot(IPLogger logger)
    {
        this.logger = logger;
    }

    @Override
    public void sendMessage(Level level, String message)
    {
        logger.logMessage(level, ChatColor.stripColor(message));
    }
}
