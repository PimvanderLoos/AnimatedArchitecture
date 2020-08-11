package nl.pim16aap2.bigdoors.tooluser;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.stepexecutor.StepExecutor;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    default String getMessage(final @NotNull Creator creator)
    {
        List<String> variables = populateVariables(creator);

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
     * Gets all the variables that will be put in the current {@link Message} as strings.
     *
     * @param creator The {@link Creator} for which to get the variables.
     */
    List<String> populateVariables(final @NotNull Creator creator);
}
