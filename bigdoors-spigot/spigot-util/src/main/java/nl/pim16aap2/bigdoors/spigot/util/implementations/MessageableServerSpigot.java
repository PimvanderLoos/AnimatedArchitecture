package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.api.IMessageable;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Represents the server as an {@link IMessageable}.
 *
 * @author Pim
 */
public final class MessageableServerSpigot implements IMessageable
{
    @NotNull
    private static final MessageableServerSpigot INSTANCE = new MessageableServerSpigot();

    private MessageableServerSpigot()
    {
    }

    /**
     * @return The instance of the server.
     */
    public @NotNull
    static MessageableServerSpigot get()
    {
        return INSTANCE;
    }

    @Override
    public void sendMessage(final @NotNull Level level, final @NotNull String message)
    {
//        PLogger.get().logMessage(ChatColor.stripColor(message), level);
    }
}
