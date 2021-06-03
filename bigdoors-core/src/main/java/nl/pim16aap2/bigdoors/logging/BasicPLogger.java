package nl.pim16aap2.bigdoors.logging;

import org.jetbrains.annotations.NotNull;

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
    private @NotNull Level logLevel = Level.CONFIG;

    private final @NotNull Consumer<String> stringConsumer;

    public BasicPLogger(final @NotNull Consumer<String> stringConsumer)
    {
        this.stringConsumer = stringConsumer;
    }

    public BasicPLogger()
    {
        this(System.out::println);
    }

    @Override
    public boolean loggable(@NotNull Level level)
    {
        return level.intValue() >= logLevel.intValue();
    }

    private void writeMessage(@NotNull Level level, @NotNull LogMessage logMessage)
    {
        if (loggable(level))
            stringConsumer.accept(logMessage.toString());
    }

    private void writeMessage(@NotNull Level level, @NotNull Supplier<LogMessage> logMessage)
    {
        if (loggable(level))
            stringConsumer.accept(logMessage.get().toString());
    }

    private void writeMessage(@NotNull Level level, @NotNull String msg)
    {
        if (loggable(level))
            writeMessage(level, new LogMessage.LogMessageString(msg, level));
    }

    @Override
    public void dumpStackTrace(@NotNull String message)
    {
        writeMessage(Level.SEVERE, new LogMessage.LogMessageStackTrace(Thread.currentThread().getStackTrace(),
                                                                       message, Level.SEVERE, 2));
    }

    @Override
    public void dumpStackTrace(@NotNull Level level, @NotNull String message)
    {
        writeMessage(level, new LogMessage.LogMessageStackTrace(Thread.currentThread().getStackTrace(),
                                                                message, level, 2));
    }

    @Override
    public void writeToConsole(@NotNull Level level, @NotNull LogMessage logMessage)
    {
        writeMessage(level, logMessage);
    }

    @Override
    public void logMessage(@NotNull Level level, @NotNull String msg)
    {
        writeMessage(level, msg);
    }

    @Override
    public void logThrowableSilently(@NotNull Throwable throwable, @NotNull String message)
    {
        logThrowableSilently(Level.SEVERE, throwable, message);
    }

    @Override
    public void logThrowableSilently(@NotNull Level level, @NotNull Throwable throwable, @NotNull String message)
    {
        writeMessage(level, message + ", " + throwable.getMessage());
    }

    @Override
    public void logThrowableSilently(@NotNull Throwable throwable)
    {
        logThrowableSilently(Level.SEVERE, throwable);
    }

    @Override
    public void logThrowableSilently(@NotNull Level level, @NotNull Throwable throwable)
    {
        writeMessage(level, throwable.getMessage());
    }

    @Override
    public void logThrowable(@NotNull Level level, @NotNull Throwable throwable, @NotNull String message)
    {
        if (loggable(level))
            throw new RuntimeException(message, throwable);
    }

    @Override
    public void logThrowable(@NotNull Throwable throwable, @NotNull String message)
    {
        if (loggable(Level.SEVERE))
            throw new RuntimeException(message, throwable);
    }

    @Override
    public void logThrowable(@NotNull Level level, @NotNull Throwable throwable)
    {
        if (loggable(level))
            throw new RuntimeException(throwable);
    }

    @Override
    public void logThrowable(@NotNull Throwable throwable)
    {
        if (loggable(Level.SEVERE))
            throw new RuntimeException(throwable);
    }

    @Override
    public void logMessage(@NotNull Level level, @NotNull String message, @NotNull Supplier<String> messageSupplier)
    {
        if (loggable(level))
            writeMessage(level, message + messageSupplier.get());
    }

    @Override
    public void logMessage(@NotNull Level level, @NotNull Supplier<String> messageSupplier)
    {
        if (loggable(level))
            writeMessage(level, messageSupplier.get());
    }

    @Override
    public void setConsoleLogLevel(@NotNull Level consoleLogLevel)
    {
        logLevel = consoleLogLevel;
    }

    /**
     * Because this is just a basic {@link IPLogger} that cannot log to a file, this method does not do anything.
     */
    @Override
    public void setFileLogLevel(@NotNull Level fileLogLevel)
    {
    }
}
