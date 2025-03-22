package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * An exception that is thrown when a player tries to execute a non-player command.
 */
public class PlayerExecutingNonPlayerCommandException extends CommandExecutionException
{
    @SuppressWarnings("unused")
    public PlayerExecutingNonPlayerCommandException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public PlayerExecutingNonPlayerCommandException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public PlayerExecutingNonPlayerCommandException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public PlayerExecutingNonPlayerCommandException(
        boolean userInformed,
        @Nullable String message,
        @Nullable Throwable cause)
    {
        super(userInformed, message, cause);
    }
}
