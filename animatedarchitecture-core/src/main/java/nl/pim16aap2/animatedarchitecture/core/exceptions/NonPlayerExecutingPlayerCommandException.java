package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * An exception that is thrown when a non-player (e.g. the server) tries to execute a player command.
 */
public class NonPlayerExecutingPlayerCommandException extends CommandExecutionException
{
    @SuppressWarnings("unused")
    public NonPlayerExecutingPlayerCommandException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public NonPlayerExecutingPlayerCommandException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public NonPlayerExecutingPlayerCommandException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public NonPlayerExecutingPlayerCommandException(
        boolean userInformed,
        @Nullable String message,
        @Nullable Throwable cause)
    {
        super(userInformed, message, cause);
    }
}
