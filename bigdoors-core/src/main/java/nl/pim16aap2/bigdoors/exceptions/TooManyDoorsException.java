package nl.pim16aap2.bigdoors.exceptions;

public class TooManyDoorsException extends Exception
{
    public TooManyDoorsException()
    {
        super();
    }

    public TooManyDoorsException(String msg)
    {
        super(msg);
    }
}
