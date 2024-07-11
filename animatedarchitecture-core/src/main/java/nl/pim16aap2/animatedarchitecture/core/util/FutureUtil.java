package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.experimental.UtilityClass;
import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Utility class for futures.
 */
@Flogger
@UtilityClass
public final class FutureUtil
{
    /**
     * See {@link #getAllCompletableFutureResults(CompletableFuture[])}.
     */
    public static <T> CompletableFuture<List<T>> getAllCompletableFutureResults(
        Collection<CompletableFuture<T>> futures)
    {
        //noinspection unchecked
        return getAllCompletableFutureResults(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Maps a group of CompletableFutures to a single CompletableFuture with a list of results.
     * <p>
     * The result will wait for each of the futures to complete and once all of them have completed gather the results
     * and return the list.
     * <p>
     * Each entry in the list maps to the result of a single future.
     *
     * @param futures
     *     The completable futures whose results to collect into a list.
     * @param <T>
     *     The type of data.
     * @return The list of results obtained from the CompletableFutures in the same order as provided. The list will
     * have a size that matches the number of input futures.
     */
    @SafeVarargs
    public static <T> CompletableFuture<List<T>> getAllCompletableFutureResults(CompletableFuture<T>... futures)
    {
        final CompletableFuture<Void> result = CompletableFuture.allOf(futures);
        return result.thenApply(ignored ->
        {
            final List<T> ret = new ArrayList<>(futures.length);
            for (final CompletableFuture<T> future : futures)
            {
                ret.add(future.join());
            }
            return ret;
        }).exceptionally(throwable -> exceptionally(throwable, Collections.emptyList()));
    }

    /**
     * See {@link #getAllCompletableFutureResultsFlatMap(CompletableFuture[])}.
     */
    public static <T> CompletableFuture<List<T>> getAllCompletableFutureResultsFlatMap(
        Collection<CompletableFuture<? extends Collection<T>>> futures)
    {
        //noinspection unchecked
        return getAllCompletableFutureResultsFlatMap(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Maps a group of CompletableFutures that each have a list as result to a single CompletableFuture with a list.
     * <p>
     * The result will wait for each of the futures to complete and once all of them have completed gather the results
     * and return the list.
     * <p>
     * The results of the futures are flatMapped into a single list.
     *
     * @param futures
     *     The completable futures whose results to flatMap into a list.
     * @param <T>
     *     The type of data in the lists.
     * @return The list of results obtained from the CompletableFutures.
     */
    @SafeVarargs
    public static <T> CompletableFuture<List<T>> getAllCompletableFutureResultsFlatMap(
        CompletableFuture<? extends Collection<T>>... futures)
    {
        final CompletableFuture<Void> result = CompletableFuture.allOf(futures);
        return result.thenApply(ignored ->
        {
            final List<T> ret = new ArrayList<>();
            for (final CompletableFuture<? extends Collection<T>> future : futures)
                ret.addAll(future.join());
            return ret;
        }).exceptionally(throwable -> exceptionally(throwable, Collections.emptyList()));
    }

    /**
     * Logs a throwable and returns a fallback value.
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
        log.atSevere().withCause(throwable).log();
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
     * See {@link #exceptionally(Throwable, Object)} with a fallback value of {@link Collections#emptyList()}.
     *
     * @return Always {@link Collections#emptyList()}.
     */
    public <T> List<T> exceptionallyList(Throwable throwable)
    {
        return exceptionally(throwable, Collections.emptyList());
    }

    /**
     * Handles exceptional completion of a {@link CompletableFuture}. This ensures that the target is finished
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
     * Handles exceptional completion of a {@link CompletableFuture}. This ensures that the target is finished
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