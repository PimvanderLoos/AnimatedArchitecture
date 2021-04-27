package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.logging.Level;

/**
 * Represents an executor for a single step in a larger procedure.
 *
 * @author Pim
 */
public abstract class StepExecutor
{
    /**
     * Applies an object to the {@link BiFunction} of this step.
     *
     * @param input The object to give to the {@link BiFunction}.
     */
    public final boolean apply(final @Nullable Object input)
    {
        if (validInput(input))
            //noinspection ConstantConditions
            return protectedAccept(input);
        else
        {
            BigDoors.get().getPLogger()
                    .logMessage(Level.FINE,
                                "Trying to pass a(n) " + (input == null ? "null" : input.getClass().getSimpleName()) +
                                    " into " + getInputClass().getSimpleName());
            return false;
        }
    }

    /**
     * Protected version of {@link #apply(Object)}. That method takes care of input type verification.
     *
     * @param obj The object to give to the {@link BiFunction}.
     */
    protected abstract boolean protectedAccept(final @NonNull Object obj);

    /**
     * Checks if an object is a valid input type.
     *
     * @param obj The object to check.
     * @return True if this object is valid for the current type.
     */
    public boolean validInput(final @Nullable Object obj)
    {
        return getInputClass().isInstance(obj);
    }

    /**
     * Checks the type that is expected as input for this step.
     *
     * @return The {@link Class} of the input object.
     */
    protected abstract @NonNull Class<?> getInputClass();
}
