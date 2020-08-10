package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Represents a step where the input is a boolean value.
 *
 * @author Pim
 */
@AllArgsConstructor
public class StepBoolean extends Step
{
    @NotNull
    private final Function<Boolean, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @Nullable Object input)
    {
        return fun.apply((Boolean) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return Boolean.class;
    }
}
