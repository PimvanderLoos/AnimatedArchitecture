package nl.pim16aap2.bigdoors.util.delayedinput;

import lombok.AccessLevel;
import lombok.Getter;
import nl.pim16aap2.bigdoors.BigDoors;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a request for delayed input. E.g. by waiting for user input.
 *
 * @param <T>
 *     The type of data to request.
 */
public abstract class DelayedInputRequest<T>
{
    private final AtomicBoolean timedOut = new AtomicBoolean(false);
    private final AtomicBoolean exceptionally = new AtomicBoolean(false);

    /**
     * The result of this input request.
     */
    @Getter(AccessLevel.PROTECTED)
    private final CompletableFuture<Optional<T>> inputResult;

    /**
     * The completable future that waits for the delayed input.
     */
    private final CompletableFuture<@Nullable T> input = new CompletableFuture<>();

    /**
     * Instantiates a new {@link DelayedInputRequest}.
     *
     * @param timeout
     *     The timeout to wait before giving up. Must be larger than 0.
     * @param timeUnit
     *     The unit of time.
     */
    protected DelayedInputRequest(long timeout, TimeUnit timeUnit)
    {
        final long timeoutMillis = timeUnit.toMillis(timeout);
        if (timeoutMillis < 1)
            throw new RuntimeException("Timeout must be larger than 0!");
        inputResult = waitForResult(timeoutMillis);
    }

    /**
     * Instantiates a new {@link DelayedInputRequest}.
     *
     * @param timeout
     *     The amount of time to wait before cancelling the request.
     */
    @SuppressWarnings("unused")
    protected DelayedInputRequest(Duration timeout)
    {
        this(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Instantiates a new {@link DelayedInputRequest}.
     *
     * @param timeout
     *     The timeout (in ms) to wait before giving up. Must be larger than 0.
     */
    protected DelayedInputRequest(long timeout)
    {
        this(timeout, TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("NullAway") // NullAway doesn't like @Nullable in the input's generics.
    private Optional<T> blockingWaitForInput(long timeout)
        throws ExecutionException, InterruptedException, TimeoutException
    {
        return Optional.ofNullable(input.get(timeout, TimeUnit.MILLISECONDS));
    }

    private CompletableFuture<Optional<T>> waitForResult(long timeout)
    {
        return CompletableFuture
            .supplyAsync(
                () ->
                {
                    try
                    {
                        return blockingWaitForInput(timeout);
                    }
                    catch (TimeoutException e)
                    {
                        timedOut.set(true);
                        return Optional.<T>empty();
                    }
                    catch (CancellationException e)
                    {
                        return Optional.<T>empty();
                    }
                    catch (Exception t)
                    {
                        exceptionally.set(true);
                        throw new RuntimeException(t);
                    }
                })
            .thenApply(
                result ->
                {
                    cleanup();
                    return result;
                })
            .exceptionally(
                ex ->
                {
                    BigDoors.get().getPLogger().logThrowable(ex);
                    exceptionally.set(true);
                    return Optional.empty();
                });
    }

    /**
     * Cancels the request if it is still waiting for input.
     * <p>
     * Calling this method after the request has already completed has not effect.
     * <p>
     * See {@link #completed()}.
     */
    public final synchronized void cancel()
    {
        inputResult.cancel(true);
    }

    /**
     * Provides the value that this object is waiting for.
     * <p>
     * Calling this method after the request has already completed has not effect.
     * <p>
     * See {@link #completed()}.
     *
     * @param value
     *     The new value.
     */
    @SuppressWarnings("NullAway")
    public final synchronized void set(@Nullable T value)
    {
        input.complete(value);
    }

    /**
     * Runs after the request has closed. Either because input was provided or because the request timed out.
     * <p>
     * See {@link #getStatus()}.
     */
    protected void cleanup()
    {
    }

    /**
     * Checks if the input request was cancelled.
     *
     * @return True if the input request was cancelled.
     */
    public boolean cancelled()
    {
        return inputResult.isCancelled();
    }

    /**
     * Checks if the input request was completed. This includes via timing out / cancellation / exception / success.
     *
     * @return True if the input request was completed.
     */
    public boolean completed()
    {
        return inputResult.isDone();
    }

    /**
     * Check if the request timed out while waiting for input.
     *
     * @return True if the request timed out.
     */
    public boolean timedOut()
    {
        return timedOut.get();
    }

    /**
     * Checks if the request was completed with an exception.
     *
     * @return True  if the request was completed with an exception.
     */
    public boolean exceptionally()
    {
        return exceptionally.get();
    }

    /**
     * Checks if the request was fulfilled successfully.
     *
     * @return True if the request was completed successfully.
     */
    public boolean success()
    {
        return completed() && !cancelled() && !timedOut() && !exceptionally();
    }

    /**
     * Gets the current status of this request.
     *
     * @return The current status of this request.
     */
    public synchronized Status getStatus()
    {
        if (cancelled())
            return Status.CANCELLED;
        if (timedOut())
            return Status.TIMED_OUT;
        if (exceptionally())
            return Status.EXCEPTION;
        return completed() ? Status.COMPLETED : Status.WAITING;
    }

    /**
     * Represents the various different stages of a request.
     */
    public enum Status
    {
        /**
         * The request is waiting for input.
         */
        WAITING,

        /**
         * The request received input and everything went as expected.
         */
        COMPLETED,

        /**
         * The request timed out before it received input.
         */
        TIMED_OUT,

        /**
         * The request was cancelled.
         */
        CANCELLED,

        /**
         * An exception occurred while trying to complete the request.
         */
        EXCEPTION
    }
}
