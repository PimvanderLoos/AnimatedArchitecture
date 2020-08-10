package nl.pim16aap2.bigdoors.tooluser;

import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a series of {@link StepExecutor}s that together form a procedure.
 *
 * @author Pim
 */
public interface IStep
{
    /**
     * Gets the localized message associated with the current {@link StepExecutor}.
     *
     * @return The message associated with the current {@link StepExecutor}.
     */
    @NotNull
    String getMessage(final @NotNull Creator creator);
}
