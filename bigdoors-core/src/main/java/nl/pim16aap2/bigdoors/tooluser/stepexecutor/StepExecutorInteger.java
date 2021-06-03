package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@ToString
@AllArgsConstructor
public class StepExecutorInteger extends StepExecutor
{
    @ToString.Exclude
    private final @NotNull Function<Integer, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NotNull Object input)
    {
        return fun.apply((Integer) input);
    }

    @Override
    public @NotNull Class<?> getInputClass()
    {
        return Integer.class;
    }
}
