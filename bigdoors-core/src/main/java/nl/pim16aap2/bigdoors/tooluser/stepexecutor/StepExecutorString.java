package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.function.Function;

@AllArgsConstructor
public class StepExecutorString extends StepExecutor
{
    private final @NonNull Function<String, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NonNull Object input)
    {
        return fun.apply((String) input);
    }

    @Override
    public @NonNull Class<?> getInputClass()
    {
        return String.class;
    }
}
