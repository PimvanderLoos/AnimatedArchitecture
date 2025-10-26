package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jspecify.annotations.Nullable;

/**
 * Represents an exception that is thrown when a user tries to access a hidden property.
 */
public class PropertyIsHiddenException extends CommandExecutionException
{
    @SuppressWarnings("unused")
    public PropertyIsHiddenException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public PropertyIsHiddenException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public PropertyIsHiddenException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public PropertyIsHiddenException(boolean userInformed, @Nullable String message, @Nullable Throwable cause)
    {
        super(userInformed, message, cause);
    }
}
