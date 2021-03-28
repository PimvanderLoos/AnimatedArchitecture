package nl.pim16aap2.bigdoors.spigot.util.implementations;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IMessageable;
import org.bukkit.ChatColor;
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
    public static @NotNull MessageableServerSpigot get()
    {
        return INSTANCE;
    }

    @Override
    public void sendMessage(final @NotNull Level level, final @NotNull String message)
    {
        BigDoors.get().getPLogger().logMessage(level, ChatColor.stripColor(message));
    }
}
