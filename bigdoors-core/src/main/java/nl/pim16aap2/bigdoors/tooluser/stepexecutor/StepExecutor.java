package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.logging.Level;

/**
 * Represents an executor for a single step in a larger procedure.
 *
 * @author Pim
 */
@AllArgsConstructor
public abstract class StepExecutor
{
    protected final IPLogger logger;

    /**
     * Applies an object to the {@link BiFunction} of this step.
     *
     * @param input
     *     The object to give to the {@link BiFunction}.
     */
    public final boolean apply(@Nullable Object input)
    {
        if (validInput(input))
            return protectedAccept(input);
        else
        {
            logger.logMessage(Level.FINE,
                              "Trying to pass a(n) " + (input == null ? "null" : input.getClass().getSimpleName()) +
                                  " into " + getInputClass().getSimpleName());
            return false;
        }
    }

    /**
     * Protected version of {@link #apply(Object)}. That method takes care of input type verification.
     *
     * @param obj
     *     The object to give to the {@link BiFunction}.
     */
    protected abstract boolean protectedAccept(@Nullable Object obj);

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
}
