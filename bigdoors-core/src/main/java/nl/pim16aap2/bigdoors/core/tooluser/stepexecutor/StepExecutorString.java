package nl.pim16aap2.bigdoors.core.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
public class StepExecutorString extends StepExecutor
{
    @ToString.Exclude
    private final Function<String, Boolean> fun;

    public StepExecutorString(Function<String, Boolean> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "String input");
        return fun.apply((String) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return String.class;
    }
}
