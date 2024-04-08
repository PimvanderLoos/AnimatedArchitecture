package nl.pim16aap2.animatedarchitecture.spigot.core.animation.recovery;

/**
 * Represents an exception that is thrown when a recovery action fails.
 */
public class RecoveryFailureException extends Exception
{
    @SuppressWarnings("unused")
    public RecoveryFailureException(String message)
    {
        super(message);
    }

    @SuppressWarnings("unused")
    public RecoveryFailureException(String message, Throwable cause)
    {
        super(message, cause);
    }

    @SuppressWarnings("unused")
    public RecoveryFailureException(Throwable cause)
    {
        super(cause);
    }
}
