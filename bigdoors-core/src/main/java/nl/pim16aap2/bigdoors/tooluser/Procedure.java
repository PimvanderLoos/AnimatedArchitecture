package nl.pim16aap2.bigdoors.tooluser;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

@ToString
public final class Procedure
{
    @Getter
    private @Nullable IStep currentStep;

    @ToString.Exclude
    protected final @NonNull ToolUser toolUser;

    final @NonNull Iterator<IStep> steps;

    public Procedure(final @NonNull ToolUser toolUser, final @NonNull List<IStep> steps)
    {
        this.toolUser = toolUser;
        this.steps = steps.iterator();
        goToNextStep();
    }

    /**
     * Checks if another step exists after the current one.
     *
     * @return True if the current step is followed by another one. When false, the current step is the final step.
     */
    public boolean hasNextStep()
    {
        return steps.hasNext();
    }

    /**
     * Advances to the next step.
     */
    public void goToNextStep()
    {
        if (!steps.hasNext())
        {
            BigDoors.get().getPLogger().logThrowable(new IndexOutOfBoundsException(
                "Trying to advance to the next step while there is none! Step: " +
                    (currentStep == null ? "NULL" : getCurrentStepName())));
            return;
        }
        currentStep = steps.next();

        if (currentStep.skip())
            goToNextStep();
    }

    /**
     * Skips to a specific {@link IStep} in this {@link Procedure}.
     * <p>
     * If the step could not be found, the procedure will skip to the last step.
     *
     * @param goalStep The {@link IStep} to jump to.
     * @return True if the jump was successful, otherwise false.
     */
    public boolean skipToStep(final @NonNull IStep goalStep)
    {
        while (steps.hasNext())
        {
            IStep step = steps.next();
            if (step.equals(goalStep))
            {
                currentStep = step;
                return true;
            }
        }
        return false;
    }

    /**
     * Applies some kind of input to the {@link StepExecutor} for the current step.
     *
     * @param obj The input to apply.
     * @return True if the application was successful.
     */
    public boolean applyStepExecutor(final @Nullable Object obj)
    {
        if (currentStep == null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new IllegalStateException("Cannot apply step executor because there is no active step!"));
            return false;
        }
        return currentStep.getStepExecutor().map(stepExecutor -> stepExecutor.apply(obj)).orElse(false);
    }

    /**
     * Gets the message for the current step, with all the variables filled in.
     *
     * @return The message for the current step.
     */
    public @NonNull String getMessage()
    {
        if (currentStep == null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new IllegalStateException("Cannot get the current step message because there is no active step!"));
            // TODO: Localization
            return "An error occurred!";
        }
        return currentStep.getLocalizedMessage();
    }

    /**
     * Gets the name of the current step.
     *
     * @return The name of the current step.
     */
    public @NonNull String getCurrentStepName()
    {
        if (currentStep == null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new IllegalStateException("Cannot get the name of the current because there is no active step!"));
            return "NULL";
        }
        return currentStep.getName();
    }

    /**
     * Whether the current step requires waiting for user input.
     *
     * @return True if the current step should wait for user input.
     */
    public boolean waitForUserInput()
    {
        if (currentStep == null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new IllegalStateException("Cannot wait for user input because there is no active step!"));
            return false;
        }
        return currentStep.waitForUserInput();
    }

    /**
     * Whether the current step should continue to the next step if execution was successful.
     * <p>
     * If no next step (see {@link #hasNextStep()}) is available, this will always return false.
     * <p>
     * If this is false, the procedure should not be moved to the next step automatically, as this is supposed to be
     * handled explicitly by the step executor itself.
     *
     * @return True if the current step should continue to the next step if execution was successful.
     */
    public boolean implicitNextStep()
    {
        if (!hasNextStep())
            return false;

        if (currentStep == null)
        {
            BigDoors.get().getPLogger().logThrowable(
                new IllegalStateException("Cannot check for implicit next step as there is no current step!"));
            return false;
        }
        return currentStep.isImplicitNextStep();
    }
}
