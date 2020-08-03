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
    void sendMessage(final @NotNull Level level, final @NotNull String message);
}
