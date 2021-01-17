package nl.pim16aap2.bigdoors.util.delayedinput;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a request for delayed input. E.g. by waiting for user input.
 *
 * @param <T> The type of data to request.
 */
public abstract class DelayedInputRequest<T>
{
    /**
     * Object used for waiting/notifying.
     */
    private final Object guard = new Object();

    /**
     * Gets the current {@link Status} of the request.
     */
    @Getter
    private volatile Status status = Status.INACTIVE;

    /**
     * The input that may be received in the future.
     */
    private @Nullable T value = null;

    /**
     * The amount of time (in ms) to wait for input.
     */
    protected final long timeout;

    /**
     * Instantiates a new {@link DelayedInputRequest}. The request itself is not placed until {@link #waitForInput()} is
     * called.
     *
     * @param timeout The timeout (in ms) to wait before giving up. Must be larger than 0.
     */
    protected DelayedInputRequest(final long timeout)
    {
        if (timeout < 1)
        {
            final IllegalArgumentException e = new IllegalArgumentException("Timeout must be larger than 0!");
            PLogger.get().logThrowableSilently(e);
            throw e;
        }
        this.timeout = timeout;
    }

    /**
     * Initializes the request (see {@link #init()}) and waits until either {@link #timeout} is reached or input is
     * received.
     * <p>
     * Note that this will block the current thread until either one of the exit conditions is met.
     * <p>
     * Calling this method more than once for the same {@link DelayedInputRequest} instance is not allowed and will
     * throw a {@link IllegalStateException}.
     *
     * @return The value that was received. If no value was received or if the value was null, an empty optional is
     * returned instead.
     */
    protected final @NonNull Optional<T> waitForInput()
    {
        synchronized (guard)
        {
            if (status != Status.INACTIVE)
            {
                final IllegalStateException e = new IllegalStateException(
                    "Trying to initialize delayed input request while it has status: " + status.name());
                PLogger.get().logThrowableSilently(e);
                throw e;
            }

            init();

            status = Status.WAITING;
            try
            {
                guard.wait(timeout);
            }
            catch (InterruptedException e)
            {
                PLogger.get().logThrowableSilently(e, "Interrupted while waiting for input!");
                Thread.currentThread().interrupt();
            }

            if (status != Status.COMPLETED && status != Status.CANCELLED)
                status = Status.TIMED_OUT;

            cleanup();

            return Optional.ofNullable(value);
        }
    }

    /**
     * Cancels the request.
     */
    public final void cancel()
    {
        synchronized (guard)
        {
            if (status == Status.WAITING)
            {
                value = null;
                guard.notify();
                status = Status.CANCELLED;
                cleanup();
            }
        }
    }

    /**
     * Provides the value that this object is waiting for.
     *
     * @param value The new value.
     */
    public final void set(final @Nullable T value)
    {
        synchronized (guard)
        {
            this.value = value;
            guard.notify();
            status = Status.COMPLETED;
        }
    }

    /**
     * Initializes a process that may result in providing input to fulfill this request.
     */
    protected abstract void init();

    /**
     * Runs after the request has closed. Either because input was provided or because the request timed out.
     * <p>
     * See {@link #getStatus()}.
     */
    protected void cleanup()
    {
    }

    /**
     * Represents the various different stages of a request.
     */
    public enum Status
    {
        /**
         * The request has not been made yet.
         */
        INACTIVE,

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
        CANCELLED
    }
}
