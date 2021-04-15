package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@AllArgsConstructor
public class StepExecutorBoolean extends StepExecutor
{
    private final @NonNull Function<Boolean, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @Nullable Object input)
    {
        return fun.apply((Boolean) input);
    }

    @Override
    public @NonNull Class<?> getInputClass()
    {
        return Boolean.class;
    }
}
