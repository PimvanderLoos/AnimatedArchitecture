package nl.pim16aap2.bigdoors.core.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
public class StepExecutorInteger extends StepExecutor
{
    @ToString.Exclude
    private final Function<Integer, Boolean> fun;

    public StepExecutorInteger(Function<Integer, Boolean> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "Integer input");
        return fun.apply((Integer) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return Integer.class;
    }
}
