package nl.pim16aap2.bigdoors.tooluser.step;

import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents a series of {@link StepExecutor}s that together form a procedure.
 *
 * @author Pim
 */
public interface IStep
{
    /**
     * Gets the name of this step.
     *
     * @return The name of this step.
     */
    @NotNull
    String getName();

    /**
     * Gets the localized {@link Message} that belongs to the current {@link IStep}.
     *
     * @return The localized {@link Message} that belongs to the current {@link IStep}.
     */
    @NotNull
    String getLocalizedMessage();

    /**
     * Checks if this type of {@link IStep} waits for user input or not.
     *
     * @return True if this type of {@link IStep} waits for user input.
     */
    boolean waitForUserInput();

    /**
     * Gets the {@link StepExecutor} for the current step.
     *
     * @return The {@link StepExecutor} for the current step.
     */
    @NotNull
    Optional<StepExecutor> getStepExecutor();

    /**
     * Checks if this step should be skipped based on certain criteria defined by the implementation.
     *
     * @return True if this step should be skipped.
     */
    boolean skip();
}
