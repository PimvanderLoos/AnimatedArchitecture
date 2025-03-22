package nl.pim16aap2.animatedarchitecture.core.util.delayedinput;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Locked;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IExecutor;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a request for delayed input. E.g. by waiting for user input.
 *
 * @param <T>
 *     The type of data to request.
 */
@Flogger
@ToString
@EqualsAndHashCode
@ExtensionMethod(CompletableFutureExtensions.class)
public class DelayedInputRequest<T>
{
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Lock lock = new ReentrantLock();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final Condition inputCondition = lock.newCondition();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final IExecutor executor;

    /**
     * The result of this input request.
     */
    @Getter(AccessLevel.PROTECTED)
    private final CompletableFuture<Optional<T>> inputResult;

    private @Nullable T value;

    /**
     * Keeps track of whether the request timed out.
     */
    private volatile boolean timedOut = false;

    /**
     * Keeps track of whether the request completed with an exception.
     */
    private volatile boolean exceptionally = false;

    private volatile boolean isCancelled = false;

    private volatile boolean isDone = false;

    /**
     * Instantiates a new {@link DelayedInputRequest}.
     *
     * @param timeout
     *     The timeout to wait before giving up. Must be larger than 0.
     * @param timeUnit
     *     The unit of time.
     * @param executor
     *     The executor used for scheduling tasks.
     */
    protected DelayedInputRequest(long timeout, TimeUnit timeUnit, IExecutor executor)
    {
        this.executor = executor;
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
     * @param executor
     *     The executor used for scheduling tasks.
     */
    protected DelayedInputRequest(Duration timeout, IExecutor executor)
    {
        this(timeout.toMillis(), TimeUnit.MILLISECONDS, executor);
    }

    private CompletableFuture<Optional<T>> waitForResult(long timeout)
    {
        return CompletableFuture
            .supplyAsync(() ->
            {
                lock.lock();
                try
                {
                    long now = System.nanoTime();
                    final long deadline = now + TimeUnit.MILLISECONDS.toNanos(timeout);

                    while (!this.isDone && now < deadline)
                    {
                        //noinspection ResultOfMethodCallIgnored
                        inputCondition.await(deadline - now, TimeUnit.NANOSECONDS);
                        now = System.nanoTime();
                    }

                    this.timedOut = !this.isDone;
                    this.isDone = true;
                    return Optional.ofNullable(value);
                }
                catch (InterruptedException e)
                {
                    exceptionally = true;
                    Thread.currentThread().interrupt();
                    return Optional.<T>empty();
                }
                catch (Exception e)
                {
                    exceptionally = true;
                    throw new RuntimeException(e);
                }
                finally
                {
                    lock.unlock();
                }
            }, executor.getVirtualExecutor())
            .thenApply(result ->
            {
                cleanup();
                return result;
            })
            .withExceptionContext(() -> "Get input for DelayedInputRequest: " + this);
    }

    /**
     * Cancels the request if it is still waiting for input.
     * <p>
     * Calling this method after the request has already completed has no effect.
     * <p>
     * See {@link #completed()}.
     */
    @Locked("lock")
    public final void cancel()
    {
        if (this.isDone)
            return;
        this.isCancelled = true;
        this.isDone = true;
        this.inputCondition.signal();
    }

    /**
     * Provides the value that this object is waiting for.
     * <p>
     * Calling this method after the request has already completed has no effect.
     * <p>
     * See {@link #completed()}.
     *
     * @param value
     *     The new value.
     */
    @Locked("lock")
    public final void set(@Nullable T value)
    {
        if (this.isDone)
            throw new IllegalStateException(
                String.format("Trying to provide value '%s', but already received value '%s'!", value, this.value));
        this.isDone = true;
        this.value = value;
        inputCondition.signal();
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
        return isCancelled;
    }

    /**
     * Checks if the input request was completed. This includes via timing out / cancellation / exception / success.
     *
     * @return True if the input request was completed.
     */
    public boolean completed()
    {
        return isDone;
    }

    /**
     * Check if the request timed out while waiting for input.
     *
     * @return True if the request timed out.
     */
    public boolean timedOut()
    {
        return timedOut;
    }

    /**
     * Checks if the request was completed with an exception.
     *
     * @return True  if the request was completed with an exception.
     */
    public boolean exceptionally()
    {
        return exceptionally;
    }

    /**
     * Checks if the request was fulfilled successfully.
     *
     * @return True if the request was completed successfully.
     */
    public boolean success()
    {
        return getStatus() == Status.COMPLETED;
    }

    /**
     * Gets the current status of this request.
     *
     * @return The current status of this request.
     */
    @Locked("lock")
    public Status getStatus()
    {
        if (isCancelled)
            return Status.CANCELLED;
        if (timedOut)
            return Status.TIMED_OUT;
        if (exceptionally)
            return Status.EXCEPTION;
        return isDone ? Status.COMPLETED : Status.WAITING;
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
