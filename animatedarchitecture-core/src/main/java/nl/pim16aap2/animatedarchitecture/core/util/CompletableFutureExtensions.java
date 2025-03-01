package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.extern.flogger.Flogger;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Contains extensions for the {@link java.util.concurrent.CompletableFuture} class.
 * <p>
 * To use these extensions, you need to annotate the class in which you want to use them with
 * {@code @ExtensionMethod(CompletableFutureExtensions.class)}.
 * <p>
 * For example, if you want to use the {@link #withExceptionContext(CompletableFuture, Supplier)} method in a class
 * called {@code MyClass} to add context to exceptions thrown by a future, you would do the following:
 * <pre>{@code
 * @ExtensionMethod(CompletableFutureExtensions.class)
 * public class MyClass {
 *    public CompletableFuture<MyEntity> findById(long id) {
 *        return CompletableFuture
 *            .supplyAsync(() -> repository.findById(id))
 *            .withExceptionContext(() -> "Failed to find entity with id " + id);
 *        }
 *    }
 * }</pre>
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

    /**
     * Attaches a handler to the provided future to handle exceptions using the provided handler.
     * <p>
     * This method allows specifying a custom handler for exceptions. The original future's completion state is
     * unaffected - this method only adds a handler for exceptions.
     * <p>
     * This method is intended to be used as a terminal operation in a chain of future operations.
     * <p>
     * Example usage when using Lombok's {@code @ExtensionMethod} annotation:
     * <pre>{@code
     * CompletableFuture<Response> future = client.sendRequest();
     * future.handleExceptional(ex -> log.atSevere().withCause(ex).log("Request failed!"));
     * }</pre>
     *
     * @param future
     *     The future to monitor for exceptions.
     * @param handler
     *     The function to call with any exception that occurs in the future.
     */
    public static void handleExceptional(CompletableFuture<?> future, Consumer<Throwable> handler)
    {
        future.whenComplete((result, throwable) ->
        {
            if (throwable == null)
                return;

            handler.accept(throwable);
        });
    }

    /**
     * Attaches a handler to the provided future that logs any exceptions using the default logger.
     * <p>
     * This is a convenience method that logs exceptions at WARNING level with the message "Exception occurred in
     * CompletableFuture!". For custom (logging) behavior, use {@link #handleExceptional(CompletableFuture, Consumer)}
     * instead.
     * <p>
     * This method is intended to be used as a terminal operation in a chain of future operations.
     * <p>
     * Example usage when using Lombok's {@code @ExtensionMethod} annotation:
     * <pre>{@code
     * CompletableFuture<User> future = userService.findUser(userId);
     * future.logExceptions();
     * }</pre>
     *
     * @param future
     *     The future to monitor for exceptions.
     */
    public static void logExceptions(CompletableFuture<?> future)
    {
        handleExceptional(
            future,
            throwable -> log.atWarning().withCause(throwable).log("Exception occurred in CompletableFuture!")
        );
    }
}
