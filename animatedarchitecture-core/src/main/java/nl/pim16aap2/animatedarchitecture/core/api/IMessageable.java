package nl.pim16aap2.animatedarchitecture.core.api;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;

/**
 * Represents objects that can receive messages.
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
     * @param text
     *     The message to send. This may or may not contain formatting.
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
        sendMessage(textFactory.newText().append(message, textType));
    }

    /**
     * Sends an error message to this object.
     *
     * @param textFactory
     *     The {@link ITextFactory} to use for creating the {@link Text} object.
     * @param message
     *     The error message to send.
     */
    default void sendError(ITextFactory textFactory, String message)
    {
        sendMessage(textFactory, TextType.ERROR, message);
    }

    /**
     * Sends a success message to this object.
     *
     * @param textFactory
     *     The {@link ITextFactory} to use for creating the {@link Text} object.
     * @param message
     *     The success message to send.
     */
    default void sendSuccess(ITextFactory textFactory, String message)
    {
        sendMessage(textFactory, TextType.SUCCESS, message);
    }

    /**
     * Sends a info message to this object.
     *
     * @param textFactory
     *     The {@link ITextFactory} to use for creating the {@link Text} object.
     * @param message
     *     The info message to send.
     */
    default void sendInfo(ITextFactory textFactory, String message)
    {
        sendMessage(textFactory, TextType.INFO, message);
    }

    /**
     * Implementation of {@link IMessageable} that does not send messages anywhere.
     */
    @Flogger
    final class BlackHoleMessageable implements IMessageable
    {
        private BlackHoleMessageable()
        {
        }

        @Override
        public void sendMessage(Text text)
        {
            log.atFinest().log("Sent to black hole: %s", text);
        }
    }
}
