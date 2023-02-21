package nl.pim16aap2.animatedarchitecture.spigot.core.exceptions;

public class InitializationException extends Exception
{
    public InitializationException(String message)
    {
        super(message);
    }

    public InitializationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InitializationException(Throwable cause)
    {
        super(cause);
    }
}
