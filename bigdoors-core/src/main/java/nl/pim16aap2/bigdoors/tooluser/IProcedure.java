package nl.pim16aap2.bigdoors.tooluser;

import nl.pim16aap2.bigdoors.tooluser.creator.Creator;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a series of {@link Step}s that together form a procedure.
 *
 * @author Pim
 */
// TODO: Rename this, it is not a procedure, but a definition of steps.
public interface IProcedure
{
//    /**
//     * Gets the {@link Step} of the current part of the procedure.
//     *
//     * @return The {@link Step} of the current part of the procedure.
//     */
//    @NotNull
//    Step getStep();

    /**
     * Gets the localized message associated with the current {@link Step}.
     *
     * @return The message associated with the current {@link Step}.
     */
    @NotNull
    String getMessage(final @NotNull Creator creator);

//    /**
//     * Gets all {@link IProcedure}s in this procedure.
//     *
//     * @return All {@link IProcedure} in this procedure.
//     */
//    @NotNull
//    List<IProcedure<T>> getValues();
}
