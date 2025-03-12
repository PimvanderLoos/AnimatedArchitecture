package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * The generic exception that is thrown when a command fails to execute.
 * <p>
 * Generally, this exception should not be thrown directly, but rather one of its subclasses.
 */
public class CommandExecutionException extends RuntimeException
{
    private final boolean userInformed;

    @SuppressWarnings("unused")
    public CommandExecutionException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public CommandExecutionException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public CommandExecutionException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public CommandExecutionException(boolean userInformed, @Nullable String message, @Nullable Throwable cause)
    {
        super(message);
        super.initCause(cause);
        this.userInformed = userInformed;
    }

    /**
     * Returns whether the user has been informed about this exception.
     * <p>
     * When this is {@code true}, a message has been sent to the user when this exception was thrown. When this is
     * {@code false}, no message has been sent to the user and a generic error message should be sent.
     *
     * @return {@code true} if the user has been informed, {@code false} otherwise.
     */
    public final boolean isUserInformed()
    {
        return userInformed;
    }
}
