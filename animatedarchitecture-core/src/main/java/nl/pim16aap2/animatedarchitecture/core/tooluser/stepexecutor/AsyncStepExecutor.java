package nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A {@link StepExecutor} that executes a function asynchronously.
 *
 * @param <T>
 *     The type of the input for the function.
 */
public class AsyncStepExecutor<T> extends StepExecutor
{
    private final Class<T> inputClass;
    private final Function<T, CompletableFuture<Boolean>> fun;

    /**
     * Creates a new {@link AsyncStepExecutor}.
     *
     * @param inputClass
     *     The class of the input for the function.
     * @param fun
     *     The function to execute asynchronously that takes an input of type {@code T} and returns a
     *     {@link CompletableFuture} of {@link Boolean}.
     *     <p>
     *     The {@link CompletableFuture} should be completed with the result of the function. If the function is
     *     successful, the {@link CompletableFuture} should be completed with {@code true}. If the function is
     *     unsuccessful, the {@link CompletableFuture} should be completed with {@code false}.
     */
    public AsyncStepExecutor(Class<T> inputClass, Function<T, CompletableFuture<Boolean>> fun)
    {
        this.inputClass = inputClass;
        this.fun = fun;
    }

    @Override
    protected CompletableFuture<Boolean> protectedAcceptAsync(@Nullable Object input)
    {
        return fun.apply(inputClass.cast(input));
    }

    @Override
    protected Class<?> getInputClass()
    {
        return inputClass;
    }

    @Override
    public boolean isAsync()
    {
        return true;
    }
}
