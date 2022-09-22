package nl.pim16aap2.bigdoors.tooluser.step;

import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import org.jetbrains.annotations.Nullable;

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
    String getName();

    /**
     * Gets the localized {@link String} that belongs to the current {@link IStep}.
     *
     * @return The localized {@link String} that belongs to the current {@link IStep}.
     */
    String getLocalizedMessage();

    /**
     * Checks if this type of {@link IStep} waits for user input or not.
     *
     * @return True if this type of {@link IStep} waits for user input.
     */
    boolean waitForUserInput();

    /**
     * @return The action to be taken to prepare this step, if any.
     */
    @Nullable Runnable getStepPreparation();

    /**
     * Gets the {@link StepExecutor} for the current step.
     *
     * @return The {@link StepExecutor} for the current step.
     */
    Optional<StepExecutor> getStepExecutor();

    /**
     * Checks if this step should be skipped based on certain criteria defined by the implementation.
     *
     * @return True if this step should be skipped.
     */
    boolean skip();

    /**
     * Checks if this step should 'automatically' proceed to the next step if the result of running the
     * {@link #getStepExecutor()} is true.
     * <p>
     * See {@link StepExecutor#apply(Object)}.
     *
     * @return True if the successful execution of this step's executor should cause it to go to the next step
     * automatically.
     */
    boolean isImplicitNextStep();
}
