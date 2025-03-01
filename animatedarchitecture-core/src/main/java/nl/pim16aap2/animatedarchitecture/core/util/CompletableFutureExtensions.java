package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.extern.flogger.Flogger;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Contains extensions for the {@link java.util.concurrent.CompletableFuture} class.
 * <p>
 * To use these extensions, you need to annotate the class in which you want to use them with
 * {@code @ExtensionMethod(CompletableFutureExtensions.class)}.
 */
@Flogger
public final class CompletableFutureExtensions
{
    private CompletableFutureExtensions()
    {
    }

    /**
     * Returns a future that either completes with the result of the original future or exceptionally with a new
     * exception that includes the provided context.
     * <p>
     * In short, this method throws a new exception with the provided context if the original future completes
     * exceptionally.
     *
     * @param future
     *     The original future.
     * @param context
     *     The context to include in the exception.
     * @param <T>
     *     The type of the future.
     * @return A future that completes with the result of the original future or exceptionally with a new exception that
     * includes the provided context.
     */
    public static <T> CompletableFuture<T> withExceptionContext(
        CompletableFuture<T> future,
        Supplier<String> context)
    {
        final CompletableFuture<T> ret = new CompletableFuture<>();

        future.whenComplete((result, throwable) ->
        {
            if (throwable != null)
            {
                final String contextString = context.get();
                log.atFinest().withCause(throwable).log(
                    "Exception occurred in CompletableFuture with context: %s",
                    contextString
                );
                ret.completeExceptionally(new RuntimeException(contextString, throwable));
            }
            else
            {
                ret.complete(result);
            }
        });

        return ret;
    }
}
