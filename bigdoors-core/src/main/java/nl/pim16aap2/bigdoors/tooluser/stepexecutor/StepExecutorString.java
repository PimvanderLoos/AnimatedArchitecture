package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.ToString;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
@AllArgsConstructor
public class StepExecutorString extends StepExecutor
{
    @ToString.Exclude
    private final Function<String, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @Nullable Object input)
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
