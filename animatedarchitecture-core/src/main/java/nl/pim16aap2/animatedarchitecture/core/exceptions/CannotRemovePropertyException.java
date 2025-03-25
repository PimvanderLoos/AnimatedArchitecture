package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an exception that is thrown when trying to remove a property from a structure, but the property cannot be
 * removed.
 */
public class CannotRemovePropertyException extends CommandExecutionException
{
    @SuppressWarnings("unused")
    public CannotRemovePropertyException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public CannotRemovePropertyException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public CannotRemovePropertyException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public CannotRemovePropertyException(boolean userInformed, @Nullable String message, @Nullable Throwable cause)
    {
        super(userInformed, message, cause);
    }
}
