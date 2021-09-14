package nl.pim16aap2.bigdoors.util;

import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CompletableFutureHandler
{
    private final IPLogger logger;

    @Inject
    public CompletableFutureHandler(IPLogger logger)
    {
        this.logger = logger;
    }

    /**
     * Logs a throwable using {@link IPLogger#logThrowable(Throwable)} and returns a fallback value.
     * <p>
     * Mostly useful for {@link CompletableFuture#exceptionally(Function)}.
     *
     * @param throwable
     *     The throwable to send to the logger.
     * @param fallback
     *     The fallback value to return.
     * @param <T>
     *     The type of the fallback value.
     * @return The fallback value.
     */
    @Contract("_, !null -> !null")
    public @Nullable <T> T exceptionally(Throwable throwable, @Nullable T fallback)
    {
        logger.logThrowable(throwable);
        return fallback;
    }

    /**
     * See {@link #exceptionally(Throwable, Object)} with a null fallback value.
     *
     * @return Always null
     */
    public @Nullable <T> T exceptionally(Throwable throwable)
    {
        return exceptionally(throwable, null);
    }

    /**
     * See {@link #exceptionally(Throwable, Object)} with a fallback value of {@link Optional#empty()}.
     *
     * @return Always {@link Optional#empty()}.
     */
    public <T> Optional<T> exceptionallyOptional(Throwable throwable)
    {
        return exceptionally(throwable, Optional.empty());
    }

    /**
     * Handles exceptional completion of a {@link CompletableFuture}. This ensure that the target is finished
     * exceptionally as well, to propagate the exception.
     *
     * @param throwable
     *     The {@link Throwable} to log.
     * @param fallback
     *     The fallback value to return.
     * @param target
     *     The {@link CompletableFuture} to complete.
     * @return The fallback value.
     */
    public <T, U> T exceptionallyCompletion(Throwable throwable, T fallback, CompletableFuture<U> target)
    {
        target.completeExceptionally(throwable);
        return fallback;
    }

    /**
     * Handles exceptional completion of a {@link CompletableFuture}. This ensure that the target is finished
     * exceptionally as well, to propagate the exception.
     *
     * @param throwable
     *     The {@link Throwable} to log.
     * @param target
     *     The {@link CompletableFuture} to complete.
     * @return Always null;
     */
    public <T> Void exceptionallyCompletion(Throwable throwable, CompletableFuture<T> target)
    {
        target.completeExceptionally(throwable);
        return null;
    }
}
