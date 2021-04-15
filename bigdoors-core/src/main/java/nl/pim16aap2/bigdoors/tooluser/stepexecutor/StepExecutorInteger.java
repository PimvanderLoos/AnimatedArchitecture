package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.function.Function;

@AllArgsConstructor
public class StepExecutorInteger extends StepExecutor
{
    private final @NonNull Function<Integer, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NonNull Object input)
    {
        return fun.apply((Integer) input);
    }

    @Override
    public @NonNull Class<?> getInputClass()
    {
        return Integer.class;
    }
}
