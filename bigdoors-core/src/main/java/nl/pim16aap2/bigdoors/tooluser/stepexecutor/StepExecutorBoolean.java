package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
@AllArgsConstructor
public class StepExecutorBoolean extends StepExecutor
{
    @ToString.Exclude
    private final @NotNull Function<Boolean, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @Nullable Object input)
    {
        return fun.apply((Boolean) input);
    }

    @Override
    public @NotNull Class<?> getInputClass()
    {
        return Boolean.class;
    }
}
