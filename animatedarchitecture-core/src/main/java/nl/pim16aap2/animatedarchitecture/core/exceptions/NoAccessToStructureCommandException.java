package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an exception that is thrown when a CommandSender tries to execute a command for a structure where they are
 * not allowed to do so.
 * <p>
 * For example, a structure owner at the USER permission level trying to execute a command that requires the ADMIN
 * permission level.
 */
public class NoAccessToStructureCommandException extends CommandExecutionException
{
    @SuppressWarnings("unused")
    public NoAccessToStructureCommandException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public NoAccessToStructureCommandException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public NoAccessToStructureCommandException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public NoAccessToStructureCommandException(
        boolean userInformed,
        @Nullable String message,
        @Nullable Throwable cause)
    {
        super(userInformed, message, cause);
    }
}
