package nl.pim16aap2.animatedarchitecture.core.tooluser;

import com.google.common.flogger.StackSize;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;

/**
 * Represents a procedure as defined by a series of {@link Step}s.
 *
 * @author Pim
 */
@ToString
@Flogger
public final class Procedure
{
    @Getter
    private @Nullable Step currentStep;

    private final Iterator<Step> steps;
    private final ILocalizer localizer;
    private final ITextFactory textFactory;

    public Procedure(List<Step> steps, ILocalizer localizer, ITextFactory textFactory)
    {
        this.steps = steps.iterator();
        this.localizer = localizer;
        this.textFactory = textFactory;
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
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Trying to advance to the next step while there is none! Step: %s",
                    (currentStep == null ? "NULL" : getCurrentStepName()));
            return;
        }
        currentStep = steps.next();

        if (currentStep.skip())
            goToNextStep();
    }

    /**
     * Skips to a specific {@link Step} in this {@link Procedure}.
     * <p>
     * If the step could not be found, the procedure will skip to the last step.
     *
     * @param goalStep
     *     The {@link Step} to jump to.
     * @return True if the jump was successful, otherwise false.
     */
    public boolean skipToStep(Step goalStep)
    {
        while (steps.hasNext())
        {
            final Step step = steps.next();
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
     * @param obj
     *     The input to apply.
     * @return True if the application was successful.
     */
    public boolean applyStepExecutor(@Nullable Object obj)
    {
        if (currentStep == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Cannot apply step executor because there is no active step!");
            return false;
        }
        return currentStep.getStepExecutor().map(stepExecutor -> stepExecutor.apply(obj)).orElse(false);
    }

    /**
     * Gets the message for the current step, with all the variables filled in.
     *
     * @return The message for the current step.
     */
    public Text getMessage()
    {
        if (currentStep == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Cannot get the current step message because there is no active step!");
            return textFactory.newText().append(localizer.getMessage("constants.error.generic"), TextType.ERROR);
        }
        return currentStep.getLocalizedMessage(textFactory);
    }

    /**
     * Gets the name of the current step.
     *
     * @return The name of the current step.
     */
    public String getCurrentStepName()
    {
        if (currentStep == null)
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Cannot get the name of the current because there is no active step!");
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
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Cannot wait for user input because there is no active step!");
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
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Cannot check for implicit next step as there is no current step!");
            return false;
        }
        return currentStep.isImplicitNextStep();
    }

    /**
     * Runs the step preparation for the current step if applicable. See {@link Step#getStepPreparation()}.
     */
    public void runCurrentStepPreparation()
    {
        if (currentStep == null)
            return;
        final @Nullable Runnable preparation = currentStep.getStepPreparation();
        if (preparation == null)
            return;
        preparation.run();
    }
}
