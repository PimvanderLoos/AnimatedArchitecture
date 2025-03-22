package nl.pim16aap2.animatedarchitecture.core.util;

import lombok.extern.flogger.Flogger;
import nl.pim16aap2.util.exceptions.ContextualOperationException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
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
     * <p>
     * One important thing to note is that supplier is only called <b>when the future completes</b>. So supplier that
     * depends on some state might append different context than expected. This is even true for 'simple' suppliers such
     * as Object::toString may depend on the state of the object.
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
    @SuppressWarnings("FutureReturnValueIgnored")
    public static <T> CompletableFuture<T> withExceptionContext(
        CompletableFuture<T> future,
        Supplier<String> context)
    {
        final CompletableFuture<T> ret = new CompletableFuture<>();

        future.whenComplete((result, throwable) ->
        {
            if (throwable == null)
            {
                ret.complete(result);
                return;
            }

            final String contextString = getContextString(throwable, context);
            final var newException = newContextualOperationException(throwable, contextString);
            ret.completeExceptionally(newException);
        });

        return ret;
    }

    private static String getContextString(Throwable throwable, Supplier<String> contextSupplier)
    {
        try
        {
            return contextSupplier.get();
        }
        catch (Throwable nestedThrowable)
        {
            log.atSevere().withCause(nestedThrowable).log(
                "Failed to get context for exception: %s",
                throwable.getMessage() // Log the outer message for context.
            );
            return "Failed to get context: " + nestedThrowable.getMessage();
        }
    }

    private static ContextualOperationException newContextualOperationException(Throwable cause, String contextString)
    {
        final Throwable unwrappedCause = unwrapCompletionException(cause);
        return new ContextualOperationException(contextString, unwrappedCause);
    }

    private static Throwable unwrapCompletionException(Throwable throwable)
    {
        if ((throwable instanceof CompletionException || throwable instanceof ExecutionException) &&
            throwable.getCause() != null &&
            throwable.getCause() instanceof ContextualOperationException)
        {
            return throwable.getCause();
        }
        return throwable;
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
    @SuppressWarnings("FutureReturnValueIgnored")
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
