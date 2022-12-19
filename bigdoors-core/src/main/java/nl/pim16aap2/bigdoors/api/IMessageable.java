package nl.pim16aap2.bigdoors.api;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.text.Text;
import nl.pim16aap2.bigdoors.text.TextType;

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
     * @param message
     *     The message to send. This may or may not contain color codes.
     */
    void sendMessage(String message);

    /**
     * Sends a message to this object.
     *
     * @param text
     *     The message to send. This may or may not contain color codes.
     */
    void sendMessage(Text text);

    /**
     * Sends a message to this object.
     *
     * @param textFactory
     *     The {@link ITextFactory} to use for creating the {@link Text} object.
     * @param textType
     *     The {@link TextType} to use for the message to send.
     * @param message
     *     The message to send.
     */
    default void sendMessage(ITextFactory textFactory, TextType textType, String message)
    {
        sendMessage(textFactory.newText().add(message, textType));
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
        public void sendMessage(String message)
        {
            log.at(Level.FINEST).log("Sent to black hole: %s", message);
        }

        @Override
        public void sendMessage(Text text)
        {
            sendMessage(text.toPlainString());
        }
    }
}
