package nl.pim16aap2.bigdoors.api;


import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Represents objects that can receive messages.
 *
 * @author Pim
 */
public interface IMessageable
{
    /**
     * Sends a message to this object.
     *
     * @param level   The level of the message, if applicable. Regular users, for example, should never see this.
     * @param message The message to send. This may or may not contain color codes.
     */
    void sendMessage(@NotNull Level level, @NotNull String message);

    /**
     * Sends a message to this object. If this target supports levels, {@link Level#INFO} will be used.
     *
     * @param message The message to send. This may or may not contain color codes.
     */
    default void sendMessage(@NotNull String message)
    {
        sendMessage(Level.INFO, message);
    }
}
