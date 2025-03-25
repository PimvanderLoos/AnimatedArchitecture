package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an exception that is thrown when a user tries to edit a property, but has insufficient permissions to do
 * so.
 */
public class CannotEditPropertyException extends CommandExecutionException
{
    @SuppressWarnings("unused")
    public CannotEditPropertyException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public CannotEditPropertyException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public CannotEditPropertyException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public CannotEditPropertyException(boolean userInformed, @Nullable String message, @Nullable Throwable cause)
    {
        super(userInformed, message, cause);
    }
}
