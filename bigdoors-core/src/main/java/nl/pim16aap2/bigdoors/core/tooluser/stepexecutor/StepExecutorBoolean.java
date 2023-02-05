package nl.pim16aap2.bigdoors.core.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
public class StepExecutorBoolean extends StepExecutor
{
    @ToString.Exclude
    private final Function<Boolean, Boolean> fun;

    public StepExecutorBoolean(Function<Boolean, Boolean> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "Boolean input");
        return fun.apply((Boolean) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return Boolean.class;
    }
}
