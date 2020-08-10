package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Represents a step without any input value.
 *
 * @author Pim
 */
@AllArgsConstructor
public class StepVoid extends Step
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
