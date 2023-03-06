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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

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

    private final Map<String, Step> stepMap;

    private final Deque<Step> steps;

    @ToString.Exclude
    private final ILocalizer localizer;

    @ToString.Exclude
    private final ITextFactory textFactory;

    public Procedure(List<Step> steps, ILocalizer localizer, ITextFactory textFactory)
    {
        this.stepMap = createStepMap(steps);
        this.steps = new ArrayDeque<>(steps);
        this.localizer = localizer;
        this.textFactory = textFactory;
        goToNextStep();
    }

    /**
     * @return A list containing all steps in this procedure including any that may have been completed already.
     */
    public List<Step> getAllSteps()
    {
        return new ArrayList<>(stepMap.values());
    }

    /**
     * Retrieves a step by its {@link Step#getName()}.
     *
     * @param name
     *     The name of the step to retrieve.
     * @return The step, if it exists in this procedure.
     */
    public Optional<Step> getStepByName(String name)
    {
        return Optional.ofNullable(stepMap.get(name));
    }

    /**
     * Inserts a step before the current step and goes to the previous step.
     * <p>
     * After this call, {@link #getCurrentStep()} will return the inserted step and {@link #goToNextStep()} will proceed
     * to the previous 'current' step.
     *
     * @param step
     *     The step to insert.
     */
    public void insertStep(Step step)
    {
        if (this.currentStep != null)
            this.steps.push(this.currentStep);
        this.currentStep = step;
    }

    /**
     * Inserts a named step.
     * <p>
     * See {@link #getStepByName(String)} and {@link #insertStep(Step)}.
     *
     * @param name
     *     The name of the step to insert.
     * @throws NoSuchElementException
     *     If no step can be found by that name.
     */
    public void insertStep(String name)
    {
        final Step step = getStepByName(name)
            .orElseThrow(() -> new NoSuchElementException("Could not find step '" + name + "' in procedure: " + this));
        insertStep(step);
    }

    /**
     * Checks if another step exists after the current one.
     *
     * @return True if the current step is followed by another one. When false, the current step is the final step.
     */
    public boolean hasNextStep()
    {
        return !steps.isEmpty();
    }

    /**
     * Advances to the next step.
     */
    public void goToNextStep()
    {
        if (!hasNextStep())
        {
            log.atSevere().withStackTrace(StackSize.FULL)
               .log("Trying to advance to the next step while there is none! Step: %s",
                    (currentStep == null ? "NULL" : getCurrentStepName()));
            return;
        }
        currentStep = steps.pop();

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
        while (hasNextStep())
        {
            final Step step = steps.pop();
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

    private static Map<String, Step> createStepMap(List<Step> steps)
    {
        final Map<String, Step> ret = new LinkedHashMap<>(steps.size());
        for (final var step : steps)
            if (ret.put(step.getName(), step) != null)
                throw new IllegalArgumentException(
                    "Trying to register duplicate entries for step name: " + step.getName());
        return ret;
    }
}
