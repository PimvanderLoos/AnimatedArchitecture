package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Represents a step where the fact that there is any input at all is the input itself and therefore doesn't have a
 * value.
 *
 * @author Pim
 */
@AllArgsConstructor
public class StepConfirm extends Step
{
    @NotNull
    private final Supplier<Boolean> fun;

    @Override
    protected boolean protectedAccept(final @Nullable Object input)
    {
        return fun.get();
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
