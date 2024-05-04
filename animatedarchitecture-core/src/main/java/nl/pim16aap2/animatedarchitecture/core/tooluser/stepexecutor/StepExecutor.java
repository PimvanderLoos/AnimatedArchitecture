package nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor;

import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Represents an executor for a single step in a larger procedure.
 *
 * @author Pim
 */
@Flogger
public abstract class StepExecutor
{
    /**
     * Applies an object to the function defined for this step.
     * <p>
     * While this method returns a {@link CompletableFuture}, it is not guaranteed to be asynchronous. If the step is
     * synchronous, it will return a completed future.
     *
     * @param input
     *     The object to give to the provided function.
     */
    public final CompletableFuture<Boolean> apply(@Nullable Object input)
    {
        if (validInput(input))
        {
            if (isAsync())
                return protectedAcceptAsync(input);
            else
                return CompletableFuture.completedFuture(protectedAccept(input));
        }
        else
        {
            log.atFine().log("Trying to pass a(n) %s into %s! This is an invalid operation!",
                             (input == null ? "null" : input.getClass().getSimpleName()),
                             getInputClass().getSimpleName());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Protected version of {@link #apply(Object)}. That method takes care of input type verification.
     * <p>
     * This method should be overridden by subclasses that are synchronous.
     *
     * @param obj
     *     The object to process in this step.
     */
    protected boolean protectedAccept(@Nullable Object obj)
    {
        throw new UnsupportedOperationException("This step has no synchronous implementation!");
    }

    /**
     * Protected version of {@link #protectedAccept(Object)}. That method takes care of input type verification.
     * <p>
     * This method should be overridden by subclasses that are asynchronous.
     *
     * @param obj
     *     The object to give to the {@link BiFunction}.
     */
    protected CompletableFuture<Boolean> protectedAcceptAsync(@Nullable Object obj)
    {
        throw new UnsupportedOperationException("This step has no asynchronous implementation!");
    }

    /**
     * Checks if an object is a valid input type.
     *
     * @param obj
     *     The object to check.
     * @return True if this object is valid for the current type.
     */
    public boolean validInput(@Nullable Object obj)
    {
        return getInputClass().isInstance(obj);
    }

    /**
     * Checks the type that is expected as input for this step.
     *
     * @return The {@link Class} of the input object.
     */
    protected abstract Class<?> getInputClass();

    /**
     * Checks if this step is asynchronous.
     *
     * @return True if this step is asynchronous.
     */
    public boolean isAsync()
    {
        return false;
    }
}
