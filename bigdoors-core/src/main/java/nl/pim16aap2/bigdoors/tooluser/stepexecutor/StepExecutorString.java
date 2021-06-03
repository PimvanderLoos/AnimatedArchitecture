package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@ToString
@AllArgsConstructor
public class StepExecutorString extends StepExecutor
{
    @ToString.Exclude
    private final @NotNull Function<String, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NotNull Object input)
    {
        return fun.apply((String) input);
    }

    @Override
    public @NotNull Class<?> getInputClass()
    {
        return String.class;
    }
}
