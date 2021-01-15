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
    private final Object guard = new Object();

    @Getter
    private volatile Status status = Status.UNINITIALIZED;

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
            if (status != Status.UNINITIALIZED)
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
                PLogger.get().logThrowable(e, "Interrupted while waiting for input!");
                Thread.currentThread().interrupt();
            }

            if (status != Status.COMPLETED)
                status = Status.TIMED_OUT;

            return Optional.ofNullable(value);
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

    public enum Status
    {
        UNINITIALIZED, WAITING, COMPLETED, TIMED_OUT
    }
}
