package nl.pim16aap2.bigdoors.logging;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents a very basic implementation of {@link IPLogger}.
 * <p>
 * Everything is dumped to stdout and exceptions are simply rethrown.
 * <p>
 * It does still take the log level into account.
 *
 * @author Pim
 */
public class BasicPLogger implements IPLogger
{
    private Level logLevel = Level.CONFIG;

    private final Consumer<String> stringConsumer;

    public BasicPLogger(Consumer<String> stringConsumer)
    {
        this.stringConsumer = stringConsumer;
    }

    public BasicPLogger()
    {
        this(System.out::println);
    }

    @Override
    public boolean loggable(Level level)
    {
        return level.intValue() >= logLevel.intValue();
    }

    private void writeMessage(Level level, LogMessage logMessage)
    {
        if (loggable(level))
            stringConsumer.accept("[" + getCurrentTime() + "] " + logMessage);
    }

    private void writeMessage(Level level, @Nullable String msg)
    {
        if (loggable(level))
            writeMessage(level, new LogMessage.LogMessageString(msg == null ? "" : msg, level));
    }

    @Override
    public void dumpStackTrace(String message)
    {
        writeMessage(Level.SEVERE, new LogMessage.LogMessageStackTrace(Thread.currentThread().getStackTrace(),
                                                                       message, Level.SEVERE, 2));
    }

    @Override
    public void dumpStackTrace(Level level, String message)
    {
        writeMessage(level, new LogMessage.LogMessageStackTrace(Thread.currentThread().getStackTrace(),
                                                                message, level, 2));
    }

    @Override
    public void writeToConsole(Level level, LogMessage logMessage)
    {
        writeMessage(level, logMessage);
    }

    @Override
    public void logMessage(Level level, String msg)
    {
        writeMessage(level, msg);
    }

    @Override
    public void logThrowableSilently(Throwable throwable, String message)
    {
        logThrowableSilently(Level.SEVERE, throwable, message);
    }

    @Override
    public void logThrowableSilently(Level level, Throwable throwable, String message)
    {
        writeMessage(level, message + ", " + throwable.getMessage());
    }

    @Override
    public void logThrowableSilently(Throwable throwable)
    {
        logThrowableSilently(Level.SEVERE, throwable);
    }

    @Override
    public void logThrowableSilently(Level level, Throwable throwable)
    {
        writeMessage(level, throwable.getMessage());
    }

    @Override
    public void logThrowable(Level level, Throwable throwable, String message)
    {
        if (loggable(level))
            throw new RuntimeException(message, throwable);
    }

    @Override
    public void logThrowable(Throwable throwable, String message)
    {
        if (loggable(Level.SEVERE))
            throw new RuntimeException(message, throwable);
    }

    @Override
    public void logThrowable(Level level, Throwable throwable)
    {
        if (loggable(level))
            throw new RuntimeException(throwable);
    }

    @Override
    public void logThrowable(Throwable throwable)
    {
        if (loggable(Level.SEVERE))
            throw new RuntimeException(throwable);
    }

    @Override
    public void logMessage(Level level, String message, Supplier<String> messageSupplier)
    {
        if (loggable(level))
            writeMessage(level, message + messageSupplier.get());
    }

    @Override
    public void logMessage(Level level, Supplier<String> messageSupplier)
    {
        if (loggable(level))
            writeMessage(level, messageSupplier.get());
    }

    @Override
    public void setConsoleLogLevel(Level consoleLogLevel)
    {
        logLevel = consoleLogLevel;
    }

    /**
     * Because this is just a basic {@link IPLogger} that cannot log to a file, this method does not do anything.
     */
    @Override
    public void setFileLogLevel(Level fileLogLevel)
    {
        // Ignored; see javadoc.
    }
}
