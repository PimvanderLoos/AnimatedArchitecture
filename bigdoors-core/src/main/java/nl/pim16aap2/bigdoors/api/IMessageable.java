package nl.pim16aap2.bigdoors.api;

import lombok.extern.flogger.Flogger;

import java.util.logging.Level;

/**
 * Represents objects that can receive messages.
 *
 * @author Pim
 */
public interface IMessageable
{
    /**
     * Instance of {@link BlackHoleMessageable} that can be used in case messages should not be sent anywhere.
     */
    IMessageable NULL = new BlackHoleMessageable();

    /**
     * Sends a message to this object.
     *
     * @param level
     *     The level of the message, if applicable. Regular users, for example, should never see this.
     * @param message
     *     The message to send. This may or may not contain color codes.
     */
    void sendMessage(Level level, String message);

    /**
     * Sends a message to this object. If this target supports levels, {@link Level#INFO} will be used.
     *
     * @param message
     *     The message to send. This may or may not contain color codes.
     */
    default void sendMessage(String message)
    {
        sendMessage(Level.INFO, message);
    }

    /**
     * Implementation of {@link IMessageable} that does not send messages anywhere.
     */
    @Flogger
    class BlackHoleMessageable implements IMessageable
    {
        private BlackHoleMessageable()
        {
        }

        @Override
        public void sendMessage(Level level, String message)
        {
            log.at(Level.FINEST).log("Sent to black hole: %s", message);
        }
    }
}
