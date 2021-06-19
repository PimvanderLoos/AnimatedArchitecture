package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.ToString;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
@AllArgsConstructor
public class StepExecutorInteger extends StepExecutor
{
    @ToString.Exclude
    private final @NotNull Function<Integer, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @Nullable Object input)
    {
        Util.requireNonNull(input, "Integer input");
        return fun.apply((Integer) input);
    }

    @Override
    public @NotNull Class<?> getInputClass()
    {
        return Integer.class;
    }
}
