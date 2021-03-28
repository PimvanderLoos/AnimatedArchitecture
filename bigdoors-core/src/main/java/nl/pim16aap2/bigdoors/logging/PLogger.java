package nl.pim16aap2.bigdoors.logging;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IMessagingInterface;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Represents my logger. Logs to the console synchronously and to the log file asynchronously.
 *
 * @author Pim
 */
public final class PLogger implements IPLogger
{
    /**
     * The file to write to.
     */
    private final @NonNull File logFile;

    /**
     * The queue of {@link LogMessage}s that will be written to the log.
     */
    @NotNull
    private final BlockingQueue<LogMessage> messageQueue = new LinkedBlockingQueue<>();

    /**
     * Check if the log file could be initialized properly.
     */
    private boolean success = false;

    /**
     * Determines the log {@link Level} for this logger considering the log file. {@link Level}s with a {@link
     * Level#intValue()} lower than that of the current {@link Level} will be ignored.
     *
     * @param fileLogLevel The new log {@link Level} for logging to the logFile.
     */
    @Getter
    @NotNull
    private Level fileLogLevel = Level.FINEST;

    @Getter
    @NotNull
    private Level consoleLogLevel = Level.CONFIG;

    @Getter
    @NotNull
    private Level lowestLevel = Level.CONFIG;

    public PLogger(@NonNull File logFile)
    {
        updateLowestLevel();
        this.logFile = logFile;
        prepareLog();
        if (success)
            new Thread(this::processQueue).start();
    }

    /**
     * Checks if the {@link #messageQueue} is empty.
     *
     * @return True if the {@link #messageQueue} is empty.
     */
    public boolean isEmpty()
    {
        return messageQueue.isEmpty();
    }

    public int size()
    {
        return messageQueue.size();
    }

    /**
     * Processes the queue of messages that will be logged to the log file.
     */
    private void processQueue()
    {
        try
        {
            // Keep getting new LogMessages. It's a blocked queue, so the thread
            // will just sleep until there's a new entry if it's currently empty.
            while (true)
                writeToLog(messageQueue.take().toString());
        }
        catch (InterruptedException e)
        {
            // Yes, this can result in garbled text, as it's not run on the main thread.
            // But it shouldn't ever be reached anyway.
            System.out.println("Cannot write to log file! Please contact pim16aap2!");
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void sendMessageToTarget(final @NotNull Object target, final @NotNull Level level,
                                    final @NotNull String str)
    {
        BigDoors.get().getMessagingInterface().sendMessageToTarget(target, level, str);
    }

    /**
     * Checks if this {@link PLogger} is in a valid state and if a message at a given level should be logged at all
     * (either console or file).
     *
     * @param level The level to compare against the allowed levels.
     * @return True if the provided level should be skipped by the {@link PLogger}.
     */
    private boolean ignoreLogLevel(final @NonNull Level level)
    {
        if (!success)
            throw new IllegalStateException("PLogger was not initialized successfully!");

        return level.intValue() < lowestLevel.intValue();
    }

    /**
     * Adds a message to the queue of messages that will be written to the log file and the console. The respective log
     * levels for both methods will be checked before logging the {@link LogMessage}.
     *
     * @param logMessageSupplier The {@link Supplier} that will create the {@link LogMessage} that is to be written to
     *                           the log file and the console.
     * @param level              The level of the message (info, warn, etc).
     */
    private void addToMessageQueue(final @NotNull Level level, final @NotNull Supplier<LogMessage> logMessageSupplier)
    {
        if (ignoreLogLevel(level))
            return;

        final @NotNull LogMessage logMessage = logMessageSupplier.get();
        if (level.intValue() >= consoleLogLevel.intValue())
            writeToConsole(level, logMessage);

        if (level.intValue() >= fileLogLevel.intValue())
            messageQueue.add(logMessage);
    }

    /**
     * Adds a message to the queue of messages that will be written to the log file if {@link #fileLogLevel} permits
     * it.
     * <p>
     * It will <b>not</b> log anything to the console.
     *
     * @param logMessageSupplier The {@link Supplier} that will create the {@link LogMessage} that is to be written to
     *                           the log file.
     * @param level              The level of the message (info, warn, etc).
     */
    private void addToSilentMessageQueue(final @NotNull Level level,
                                         final @NotNull Supplier<LogMessage> logMessageSupplier)
    {
        if (ignoreLogLevel(level))
            return;

        if (level.intValue() >= fileLogLevel.intValue())
            messageQueue.add(logMessageSupplier.get());
    }

    /**
     * Creates the log file, if it doesn't exist already.
     */
    private void prepareLog()
    {
        if (!logFile.exists())
            try
            {
                if (!logFile.getParentFile().exists())
                    if (!logFile.getParentFile().mkdirs())
                    {
                        writeToConsole(Level.SEVERE,
                                       "Failed to create folder: \"" + logFile.getParentFile().toString() + "\"");
                        return;
                    }
                if (!logFile.createNewFile())
                {
                    writeToConsole(Level.SEVERE, "Failed to create file: \"" + logFile.toString() + "\"");
                    return;
                }
                writeToConsole(Level.INFO, "New file created at " + logFile);
                success = true;
            }
            catch (IOException e)
            {
                writeToConsole(Level.SEVERE, "File write error: " + logFile);
                e.printStackTrace();
                return;
            }
        success = true;
    }

    @Override
    public void dumpStackTrace(final @NotNull String message)
    {
        dumpStackTrace(Level.SEVERE, message);
    }

    @Override
    public void dumpStackTrace(final @NotNull Level level, final @NotNull String message)
    {
        addToMessageQueue(level, () -> new LogMessage.LogMessageThrowable(new Exception(), message, level));
    }

    /**
     * Writes a message of a given level to the console.
     *
     * @param level  The level of the message.
     * @param string The message to log
     * @see IMessagingInterface#writeToConsole(Level, String)
     */
    private void writeToConsole(final @NotNull Level level, final @NotNull String string)
    {
        BigDoors.get().getMessagingInterface().writeToConsole(level, string);
    }

    @Override
    public void writeToConsole(final @NotNull Level level, final @NotNull LogMessage logMessage)
    {
        writeToConsole(level, logMessage.toString());
    }

    @Override
    public void logMessage(final @NotNull Level level, final @NotNull String msg)
    {
        addToMessageQueue(level, () -> new LogMessage.LogMessageString(msg, level));
    }

    /**
     * Writes a message to the log file.
     *
     * @param msg The message to be written.
     */
    private void writeToLog(final @NotNull String msg)
    {
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true)))
        {
            bw.write("[" + IPLogger.dateFormat.format(new Date()) + "] " + msg);
            bw.flush();
        }
        catch (IOException e)
        {
            writeToConsole(Level.SEVERE, "Logging error! Could not log to logFile!");
            e.printStackTrace();
        }
    }

    private void addThrowableToQueue(final @NotNull Level level, final @NotNull Throwable throwable,
                                     final @NotNull String message)
    {
        addToMessageQueue(level, () -> new LogMessage.LogMessageThrowable(throwable, message, level));
    }

    @Override
    public void logThrowableSilently(final @NotNull Throwable throwable, final @NotNull String message)
    {
        final Level level = Level.SEVERE;
        addToSilentMessageQueue(level, () -> new LogMessage.LogMessageThrowable(throwable, message, level));
    }

    @Override
    public void logThrowableSilently(final @NotNull Level level, final @NotNull Throwable throwable,
                                     final @NotNull String message)
    {
        addToSilentMessageQueue(level, () -> new LogMessage.LogMessageThrowable(throwable, message, level));
    }

    @Override
    public void logThrowableSilently(final @NotNull Throwable throwable)
    {
        logThrowableSilently(throwable, "");
    }

    @Override
    public void logThrowableSilently(final @NotNull Level level, final @NotNull Throwable throwable)
    {
        logThrowableSilently(level, throwable, "");
    }

    @Override
    public void logThrowable(final @NotNull Level level, final @NotNull Throwable throwable,
                             final @NotNull String message)
    {
        addThrowableToQueue(level, throwable, message);

        // There's a limit to disabling stacktraces.
        // When disabled, the name of the issue is still printed!
        if (consoleLogLevel.intValue() == Level.OFF.intValue())
            writeToConsole(Level.OFF, throwable.toString());

        if (fileLogLevel.intValue() == Level.OFF.intValue())
            addToMessageQueue(Level.OFF, () -> new LogMessage.LogMessageString(throwable.toString(), level));
    }

    @Override
    public void logThrowable(final @NotNull Throwable throwable, final @NotNull String message)
    {
        logThrowable(Level.SEVERE, throwable, message);
    }

    @Override
    public void logThrowable(final @NotNull Level level, final @NotNull Throwable throwable)
    {
        logThrowable(level, throwable, "");
    }

    @Override
    public void logThrowable(final @NotNull Throwable throwable)
    {
        logThrowable(throwable, "");
    }

    @Override
    public void logMessage(final @NotNull Level level, final @NotNull String message,
                           final @NotNull Supplier<String> messageSupplier)
    {
        addToMessageQueue(level, () -> new LogMessage.LogMessageStringSupplier(message, messageSupplier, level));
    }

    @Override
    public void logMessage(final @NotNull Level level, final @NotNull Supplier<String> messageSupplier)
    {
        logMessage(level, "", messageSupplier);
    }

    @Override
    public void info(final @NotNull String str)
    {
        logMessage(Level.INFO, str);
    }

    @Override
    public void warn(final @NotNull String str)
    {
        logMessage(Level.WARNING, str);
    }

    @Override
    public void severe(final @NotNull String str)
    {
        logMessage(Level.SEVERE, str);
    }

    @Override
    public void debug(final @NotNull String str)
    {
        logMessage(Level.FINEST, str);
    }

    @Override
    public void setConsoleLogLevel(final @NotNull Level consoleLogLevel)
    {
        this.consoleLogLevel = consoleLogLevel;
        updateLowestLevel();
    }

    @Override
    public void setFileLogLevel(final @NotNull Level fileLogLevel)
    {
        this.fileLogLevel = fileLogLevel;
        updateLowestLevel();
    }

    private void updateLowestLevel()
    {
        if (fileLogLevel.intValue() > consoleLogLevel.intValue())
            lowestLevel = consoleLogLevel;
        else
            lowestLevel = fileLogLevel;
    }
}
