package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;

/**
 * Represents objects that can receive messages.
 */
public interface IMessageable
{
    /**
     * Sends a message to this object.
     *
     * @param text
     *     The message to send. This may or may not contain formatting.
     */
    void sendMessage(Text text);

    /**
     * Gets the text factory of this messageable.
     *
     * @return The text factory of this messageable.
     */
    ITextFactory getTextFactory();

    /**
     * Creates a new {@link PersonalizedLocalizer} using the localizer and locale of this messageable.
     *
     * @return A new {@link PersonalizedLocalizer} object.
     */
    PersonalizedLocalizer getPersonalizedLocalizer();

    /**
     * Creates a new {@link Text} using the text factory of this messageable.
     *
     * @return A new {@link Text} object.
     */
    default Text newText()
    {
        return getTextFactory().newText(getPersonalizedLocalizer());
    }

    /**
     * Sends a message to this object.
     * <p>
     * This is a shortcut for {@link #sendMessage(Text)}.
     *
     * @param textType
     *     The {@link TextType} to use for the message to send.
     * @param key
     *     The key of the error message. E.g. {@code constants.error.generic}.
     * @param args
     *     The arguments to use for the message.
     */
    default void sendMessage(TextType textType, String key, Text.ArgumentCreator... args)
    {
        sendMessage(newText().append(getPersonalizedLocalizer().getMessage(key), textType, args));
    }

    /**
     * Sends an error message to this messageable.
     * <p>
     * This is a shortcut for {@link #sendMessage(TextType, String, Text.ArgumentCreator...)} with the
     * {@link TextType#ERROR} type.
     *
     * @param key
     *     The key of the error message. E.g. {@code constants.error.generic}.
     * @param args
     *     The arguments to use for the message, if any.
     */
    default void sendError(String key, Text.ArgumentCreator... args)
    {
        sendMessage(TextType.ERROR, key, args);
    }

    /**
     * Sends a success message to this object.
     * <p>
     * This is a shortcut for {@link #sendMessage(TextType, String, Text.ArgumentCreator...)} with the
     * {@link TextType#SUCCESS} type.
     *
     * @param key
     *     The key of the error message. E.g. {@code constants.error.generic}.
     * @param args
     *     The arguments to use for the message, if any.
     */
    default void sendSuccess(String key, Text.ArgumentCreator... args)
    {
        sendMessage(TextType.SUCCESS, key, args);
    }

    /**
     * Sends a info message to this object.
     * <p>
     * This is a shortcut for {@link #sendMessage(TextType, String, Text.ArgumentCreator...)} with the
     * {@link TextType#INFO} type.
     *
     * @param key
     *     The key of the error message. E.g. {@code constants.error.generic}.
     * @param args
     *     The arguments to use for the message, if any.
     */
    default void sendInfo(String key, Text.ArgumentCreator... args)
    {
        sendMessage(TextType.INFO, key, args);
    }
}
