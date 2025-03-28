package nl.pim16aap2.animatedarchitecture.core.api;

import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

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
     * Gets the localizer of this messageable.
     *
     * @return The localizer of this messageable.
     */
    ILocalizer getLocalizer();

    /**
     * Gets the text factory of this messageable.
     *
     * @return The text factory of this messageable.
     */
    ITextFactory getTextFactory();

    /**
     * Gets the locale of this sender.
     * <p>
     * Defaults to null.
     *
     * @return The locale of this sender or null if no locale is set.
     */
    default @Nullable Locale getLocale()
    {
        return null;
    }

    /**
     * Creates a new {@link Text} using the text factory of this messageable.
     *
     * @return A new {@link Text} object.
     */
    default Text createText()
    {
        return getTextFactory().newText();
    }

    /**
     * Localizes a message for this messageable.
     * <p>
     * This method will use the locale of this messageable.
     *
     * @param key
     *     The key of the message.
     * @param args
     *     The arguments of the message, if any.
     * @return The localized message associated with the provided key.
     */
    default String localized(String key, Object... args)
    {
        return getLocalizer().getMessage(key, getLocale(), args);
    }

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
}
