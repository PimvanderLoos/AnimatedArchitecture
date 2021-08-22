package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IMessageable;
import org.bukkit.ChatColor;

import java.util.logging.Level;

/**
 * Represents the server as an {@link IMessageable}.
 *
 * @author Pim
 */
public final class MessageableServerSpigot implements IMessageable
{
    private static final MessageableServerSpigot INSTANCE = new MessageableServerSpigot();

    private MessageableServerSpigot()
    {
    }

    /**
     * @return The instance of the server.
     */
    public static MessageableServerSpigot get()
    {
        return INSTANCE;
    }

    @Override
    public void sendMessage(Level level, String message)
    {
        BigDoors.get().getPLogger().logMessage(level, ChatColor.stripColor(message));
    }
}
