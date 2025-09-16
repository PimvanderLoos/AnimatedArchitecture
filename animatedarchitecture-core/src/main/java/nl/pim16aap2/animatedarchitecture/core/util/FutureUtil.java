package nl.pim16aap2.animatedarchitecture.core.util;

import com.google.common.flogger.LazyArg;
import com.google.common.flogger.LazyArgs;
import com.google.common.flogger.StackSize;
import lombok.CustomLog;
import lombok.experimental.ExtensionMethod;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for futures.
 */
@CustomLog
@UtilityClass
@ExtensionMethod(CompletableFutureExtensions.class)
public final class FutureUtil
{
    /**
     * Logs a possible deadlock timeout.
     *
     * @param context
     *     The context in which the deadlock occurred.
     * @param timeoutMs
     *     The timeout in milliseconds. This is the time that the thread was waiting before considering it a deadlock.
     * @param isMainThread
     *     Whether the thread that timed out is the main thread.
     */
    public static void logPossibleDeadlockTimeout(LazyArg<?> context, int timeoutMs, boolean isMainThread)
    {
        log.atError()
            .atMostEvery(30, TimeUnit.SECONDS)
            .withStackTrace(StackSize.FULL)
            .log("""
                    Possible deadlock detected! Please contact pim16aap2 with the following information:
                    \s
                    Timeout after %dms on %s/%d (Main thread: %b)
                    Context: %s
                    \s
                    Thread dump: %s
                    """,
                timeoutMs,
                Thread.currentThread().getName(),
                Thread.currentThread().threadId(),
                isMainThread,

                context,

                LazyArgs.lazy(FutureUtil::dumpThreadStacks)
            );
    }

    /**
     * Dumps the stack traces of all threads.
     *
     * @return The stack traces of all threads represented as a string.
     */
    private static String dumpThreadStacks()
    {
        final StringBuilder dump = new StringBuilder();
        Thread.getAllStackTraces().forEach((thread, stackTrace) ->
        {
            dump.append(
                String.format("\nThread: %s (State: %s, ID: %d)\n",
                    thread.getName(),
                    thread.getState(),
                    thread.threadId())
            );

            Arrays.stream(stackTrace)
                .forEach(element -> dump
                    .append("    at ")
                    .append(element)
                    .append('\n')
                );
        });

        return dump.toString();
    }

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
        return CompletableFuture
            .allOf(futures)
            .thenApply(ignored ->
            {
                final List<T> ret = new ArrayList<>(futures.length);
                for (final CompletableFuture<T> future : futures)
                {
                    ret.add(future.join());
                }
                return ret;
            });
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
        });
    }
}
