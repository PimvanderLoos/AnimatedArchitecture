package nl.pim16aap2.animatedarchitecture.core.exceptions;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an exception that is thrown when a command requires properties and it targets a structure that doesn't
 * have those properties.
 */
public class RequiredPropertiesMissingForCommandException extends CommandExecutionException
{
    @SuppressWarnings("unused")
    public RequiredPropertiesMissingForCommandException(boolean userInformed)
    {
        this(userInformed, null, null);
    }

    @SuppressWarnings("unused")
    public RequiredPropertiesMissingForCommandException(boolean userInformed, @Nullable String message)
    {
        this(userInformed, message, null);
    }

    @SuppressWarnings("unused")
    public RequiredPropertiesMissingForCommandException(boolean userInformed, @Nullable Throwable cause)
    {
        this(userInformed, cause != null ? cause.getMessage() : null, cause);
    }

    public RequiredPropertiesMissingForCommandException(
        boolean userInformed,
        @Nullable String message,
        @Nullable Throwable cause)
    {
        super(userInformed, message, cause);
    }
}
