package nl.pim16aap2.bigdoors.tooluser;

import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

public final class Procedure<T extends ToolUser>
{
    @NotNull
    protected IStep currentStep;

    @NotNull
    protected final T toolUser;

    @NotNull
    Iterator<IStep> steps;

    public Procedure(final @NotNull T toolUser, final @NotNull List<IStep> steps)
    {
        this.toolUser = toolUser;
        this.steps = steps.iterator();
        currentStep = this.steps.next();
    }

    /**
     * Advances to the next step.
     */
    public void goToNextStep()
    {
        if (!steps.hasNext())
        {
            PLogger.get().logException(new IndexOutOfBoundsException(
                "Trying to advance to the next step while there is none! Step: " + getCurrentStepName()));
            return;
        }
        currentStep = steps.next();

        if (currentStep.skip())
            goToNextStep();
    }

    /**
     * Applies some kind of input to the {@link StepExecutor} for the current step.
     *
     * @param obj The input to apply.
     * @return True if the application was successful.
     */
    public boolean applyStepExecutor(final @Nullable Object obj)
    {
        return currentStep.getStepExecutor().map(stepExecutor -> stepExecutor.apply(obj)).orElse(false);
    }

    /**
     * Gets the message for the current step, with all the variables filled in.
     *
     * @return The message for the current step.
     */
    @NotNull
    public String getMessage()
    {
        return currentStep.getLocalizedMessage();
    }

    /**
     * Gets the name of the current step.
     *
     * @return The name of the current step.
     */
    @NotNull
    public String getCurrentStepName()
    {
        return currentStep.getName();
    }

    /**
     * Whether the current step requires waiting for user input.
     *
     * @return True if the current step should wait for user input.
     */
    public boolean waitForUserInput()
    {
        return currentStep.waitForUserInput();
    }
}
