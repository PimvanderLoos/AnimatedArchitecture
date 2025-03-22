package nl.pim16aap2.animatedarchitecture.core.exceptions;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import nl.pim16aap2.animatedarchitecture.core.api.NamespacedKey;

/**
 * Thrown when a {@link NamespacedKey} is invalid.
 */
public class InvalidNameSpacedKeyException extends RuntimeException
{
    /**
     * Creates a new {@link InvalidNameSpacedKeyException} with the given message.
     *
     * @param message
     *     The message of the exception.
     */
    public InvalidNameSpacedKeyException(String message)
    {
        super(message);
    }

    /**
     * Creates a new {@link InvalidNameSpacedKeyException} with the given message and cause.
     *
     * @param message
     *     The message of the exception.
     * @param cause
     *     The cause of the exception.
     */
    public InvalidNameSpacedKeyException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Formats the message with the given arguments.
     * <p>
     * This method is a shortcut for {@link String#format(String, Object...)}.
     *
     * @param message
     *     The message to format.
     * @param args
     *     The arguments to format the message with.
     * @return The formatted exception.
     */
    @FormatMethod
    public static InvalidNameSpacedKeyException format(@FormatString String message, Object... args)
    {
        return new InvalidNameSpacedKeyException(String.format(message, args));
    }
}
