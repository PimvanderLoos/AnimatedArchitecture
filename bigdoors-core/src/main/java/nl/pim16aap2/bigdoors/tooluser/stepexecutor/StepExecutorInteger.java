package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@AllArgsConstructor
public class StepExecutorInteger extends StepExecutor
{
    @NotNull
    private final Function<Integer, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NotNull Object input)
    {
        return fun.apply((Integer) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return Integer.class;
    }
}
