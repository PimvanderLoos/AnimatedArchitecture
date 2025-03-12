package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an exception that is thrown when a command that requires one or more structures is executed, but no
 * structures are found.
 */
public class NoStructuresForCommandException extends CommandExecutionException
{
    @SuppressWarnings("unused")
    public NoStructuresForCommandException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public NoStructuresForCommandException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public NoStructuresForCommandException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public NoStructuresForCommandException(boolean userInformed, @Nullable String message, @Nullable Throwable cause)
    {
        super(userInformed, message, cause);
    }
}
