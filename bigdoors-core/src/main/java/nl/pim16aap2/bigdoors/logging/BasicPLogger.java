package nl.pim16aap2.bigdoors.logging;

import lombok.NonNull;

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

    private Consumer<String> stringConsumer;

    public BasicPLogger(Consumer<String> stringConsumer)
    {
        this.stringConsumer = stringConsumer;
    }

    public BasicPLogger()
    {
        this(System.out::println);
    }

    private boolean loggable(@NonNull Level level)
    {
        return level.intValue() >= logLevel.intValue();
    }

    private void writeMessage(@NonNull Level level, @NonNull LogMessage logMessage)
    {
        if (loggable(level))
            stringConsumer.accept(logMessage.toString());
    }

    private void writeMessage(@NonNull Level level, @NonNull Supplier<LogMessage> logMessage)
    {
        if (loggable(level))
            stringConsumer.accept(logMessage.get().toString());
    }

    private void writeMessage(@NonNull Level level, @NonNull String msg)
    {
        if (loggable(level))
            writeMessage(level, new LogMessage.LogMessageString(msg, level));
    }

    @Override
    public void sendMessageToTarget(@NonNull Object target, @NonNull Level level, @NonNull String str)
    {
        writeMessage(level, str);
    }

    @Override
    public void dumpStackTrace(@NonNull String message)
    {
        writeMessage(Level.SEVERE, new LogMessage.LogMessageStackTrace(Thread.currentThread().getStackTrace(),
                                                                       message, Level.SEVERE, 2));
    }

    @Override
    public void dumpStackTrace(@NonNull Level level, @NonNull String message)
    {
        writeMessage(level, new LogMessage.LogMessageStackTrace(Thread.currentThread().getStackTrace(),
                                                                message, level, 2));
    }

    @Override
    public void writeToConsole(@NonNull Level level, @NonNull LogMessage logMessage)
    {
        writeMessage(level, logMessage);
    }

    @Override
    public void logMessage(@NonNull Level level, @NonNull String msg)
    {
        writeMessage(level, msg);
    }

    @Override
    public void logThrowableSilently(@NonNull Throwable throwable, @NonNull String message)
    {
        logThrowableSilently(Level.SEVERE, throwable, message);
    }

    @Override
    public void logThrowableSilently(@NonNull Level level, @NonNull Throwable throwable, @NonNull String message)
    {
        writeMessage(level, message + ", " + throwable.getMessage());
    }

    @Override
    public void logThrowableSilently(@NonNull Throwable throwable)
    {
        logThrowableSilently(Level.SEVERE, throwable);
    }

    @Override
    public void logThrowableSilently(@NonNull Level level, @NonNull Throwable throwable)
    {
        writeMessage(level, throwable.getMessage());
    }

    @Override
    public void logThrowable(@NonNull Level level, @NonNull Throwable throwable, @NonNull String message)
    {
        if (loggable(level))
            throw new RuntimeException(message, throwable);
    }

    @Override
    public void logThrowable(@NonNull Throwable throwable, @NonNull String message)
    {
        if (loggable(Level.SEVERE))
            throw new RuntimeException(message, throwable);
    }

    @Override
    public void logThrowable(@NonNull Level level, @NonNull Throwable throwable)
    {
        if (loggable(level))
            throw new RuntimeException(throwable);
    }

    @Override
    public void logThrowable(@NonNull Throwable throwable)
    {
        if (loggable(Level.SEVERE))
            throw new RuntimeException(throwable);
    }

    @Override
    public void logMessage(@NonNull Level level, @NonNull String message, @NonNull Supplier<String> messageSupplier)
    {
        if (loggable(level))
            writeMessage(level, message + messageSupplier.get());
    }

    @Override
    public void logMessage(@NonNull Level level, @NonNull Supplier<String> messageSupplier)
    {
        if (loggable(level))
            writeMessage(level, messageSupplier.get());
    }

    @Override
    public void info(@NonNull String str)
    {
        writeMessage(Level.INFO, str);
    }

    @Override
    public void warn(@NonNull String str)
    {
        writeMessage(Level.WARNING, str);
    }

    @Override
    public void severe(@NonNull String str)
    {
        writeMessage(Level.SEVERE, str);
    }

    @Override
    public void debug(@NonNull String str)
    {
        writeMessage(Level.FINEST, str);
    }

    @Override
    public void setConsoleLogLevel(@NonNull Level consoleLogLevel)
    {
        logLevel = consoleLogLevel;
    }

    /**
     * Because this is just a basic {@link IPLogger} that cannot log to a file, this method does not do anything.
     */
    @Override
    public void setFileLogLevel(@NonNull Level fileLogLevel)
    {
    }
}
