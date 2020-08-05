package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Represents a step where the fact that there is any input at all is the input itself and therefore doesn't have a
 * value.
 *
 * @author Pim
 */
@AllArgsConstructor
public class StepConfirm<T extends ToolUser<T>> extends Step<T>
{
    @NotNull
    private final Function<T, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NotNull T toolUser, final @Nullable Object input)
    {
        return fun.apply(toolUser);
    }

    @Override
    public boolean validInput(final @Nullable Object obj)
    {
        return obj == null;
    }

    @Override
    public Class<?> getInputClass()
    {
        return Object.class;
    }
}
