package nl.pim16aap2.bigdoors.tooluser;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

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
    default String getMessage(final @NotNull ToolUser toolUser)
    {
        List<String> variables = populateVariables(toolUser);

        String[] variablesArr = new String[variables.size()];
        variablesArr = variables.toArray(variablesArr);

        return BigDoors.get().getPlatform().getMessages().getString(getMessage(), variablesArr);
    }

    /**
     * Gets the {@link Message} that belongs to the current {@link IStep}.
     *
     * @return The {@link Message} that belongs to the current {@link IStep}.
     */
    @NotNull
    Message getMessage();

    /**
     * Checks if this type of {@link IStep} waits for user input or not.
     *
     * @return True if this type of {@link IStep} waits for user input.
     */
    boolean waitForUserInput();

    @NotNull
    Optional<StepExecutor> getStepExecutor(final @NotNull ToolUser toolUser);

    /**
     * Gets all the variables that will be put in the current {@link Message} as strings.
     *
     * @param toolUser The {@link ToolUser} for which to get the variables.
     * @return All the variables that will be put in the current {@link Message} as strings. If none are needed or
     * found, an empty list will be returned.
     */
    @NotNull
    List<String> populateVariables(final @NotNull ToolUser toolUser);
}
