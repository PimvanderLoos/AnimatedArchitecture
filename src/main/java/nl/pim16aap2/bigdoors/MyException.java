package nl.pim16aap2.bigdoors;

import java.io.PrintStream;
import java.io.PrintWriter;

public class MyException extends Exception
{
    private static final long serialVersionUID = 1L;

    private final Exception exception;
    private final String warning;
    private final boolean hasWarning;

    public MyException(final Exception exception, final String warning)
    {
        this.exception = exception;
        this.warning = warning;
        hasWarning = warning != null;
    }

    public MyException(final Exception exception)
    {
        this(exception, null);
    }

    public boolean hasWarningMessage()
    {
        return hasWarning;
    }

    public String getWarningMessage()
    {
        return warning;
    }

    @Override
    public void printStackTrace()
    {
        exception.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream s)
    {
        exception.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintWriter w)
    {
        exception.printStackTrace(w);
    }

    @Override
    public Throwable getCause()
    {
        return exception.getCause();
    }

    @Override
    public String getMessage()
    {
        return exception.getMessage();
    }

    @Override
    public StackTraceElement[] getStackTrace()
    {
        return exception.getStackTrace();
    }

    @Override
    public String toString()
    {
        return exception.toString();
    }
}
