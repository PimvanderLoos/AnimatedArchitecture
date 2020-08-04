package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.NonNull;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Represents a single step in a larger procedure.
 *
 * @author Pim
 */
public abstract class Step<T extends ToolUser<T>>
{
    /**
     * Applies an object to the {@link Consumer} of this step.
     *
     * @param toolUser The {@link ToolUser} for whom this action will be applied.
     * @param input    The object to give to the {@link Consumer}.
     */
    public final void accept(final @NotNull T toolUser, final @Nullable Object input)
    {
        if (validInput(input))
            protectedAccept(toolUser, input);
        else
            PLogger.get().logException(
                new IllegalArgumentException(
                    "Trying to pass a " + (input == null ? "null" : input.getClass().getSimpleName()) +
                        " into " + getInputClass().getSimpleName()));
    }

    /**
     * Protected version of {@link #accept(ToolUser, Object)}. That method takes care of input type verification.
     *
     * @param toolUser The {@link ToolUser} for whom this action will be applied.
     * @param obj      The object to give to the {@link Consumer}.
     */
    protected abstract void protectedAccept(final @NotNull T toolUser, final @NonNull Object obj);

    /**
     * Checks if an object is a valid input type.
     *
     * @param obj The object to check.
     * @return True if this object is valid for the current type.
     */
    public final boolean validInput(final @Nullable Object obj)
    {
        return getInputClass().isInstance(obj);
    }

    /**
     * Checks the type that is expected as input for this step.
     *
     * @return The {@link Class} of the input object.
     */
    public abstract Class<?> getInputClass();
}
