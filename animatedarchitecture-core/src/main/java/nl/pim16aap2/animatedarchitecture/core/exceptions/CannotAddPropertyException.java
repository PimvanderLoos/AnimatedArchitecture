package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jspecify.annotations.Nullable;

/**
 * Represents an exception that is thrown when trying to add a property to a structure, but the property cannot be
 * added.
 */
public class CannotAddPropertyException extends CommandExecutionException
{
    @SuppressWarnings("unused")
    public CannotAddPropertyException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public CannotAddPropertyException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public CannotAddPropertyException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public CannotAddPropertyException(boolean userInformed, @Nullable String message, @Nullable Throwable cause)
    {
        super(userInformed, message, cause);
    }
}
