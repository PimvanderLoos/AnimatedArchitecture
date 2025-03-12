package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an exception that is thrown when the input for a command is invalid.
 */
public class InvalidCommandInputException extends CommandExecutionException
{
    @SuppressWarnings("unused")
    public InvalidCommandInputException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public InvalidCommandInputException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public InvalidCommandInputException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public InvalidCommandInputException(boolean userInformed, @Nullable String message, @Nullable Throwable cause)
    {
        super(userInformed, message, cause);
    }
}
