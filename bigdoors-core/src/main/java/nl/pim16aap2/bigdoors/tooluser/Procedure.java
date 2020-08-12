package nl.pim16aap2.bigdoors.tooluser;

import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import nl.pim16aap2.bigdoors.util.PLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

// TODO: Consider abandoning enums. Something like this might be nice:
//       new Step(this).setFunction(this::setname).setMessage(Message.SOMETHING).addMessageVariable(this::getPrice);

public abstract class Procedure<T extends ToolUser>
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
                "Trying to advance to the next step while there is none! Step: " + getStepClassName()));
            return;
        }

        currentStep = steps.next();
    }

    /**
     * Jumps to the provided step if it has been registered in the procedure. If it hasn't been registered, it will skip
     * to the last step.
     *
     * @param step The skip to skip to.
     * @return True if the jump was successful.
     */
    public boolean skipToStep(final @NotNull IStep step)
    {
        while (steps.hasNext())
        {
            final @NotNull IStep current = steps.next();
            if (current.equals(step))
            {
                currentStep = current;
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
        return currentStep.getStepExecutor(toolUser).map(stepExecutor -> stepExecutor.apply(obj)).orElse(false);
    }

    /**
     * Gets the message for the current step, with all the variables filled in.
     *
     * @return The message for the current step.
     */
    @NotNull
    public String getMessage()
    {
        return currentStep.getMessage(toolUser);
    }

    /**
     * Gets the simple name of the class of the current step.
     *
     * @return The simple name of the class of the current step.
     */
    // TODO: Return the name of the step.
    @NotNull
    public String getStepClassName()
    {
        return currentStep.getClass().getSimpleName();
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
