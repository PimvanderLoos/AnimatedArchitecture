package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@AllArgsConstructor
public class StepExecutorPLocation extends StepExecutor
{
    @NotNull
    private final Function<IPLocationConst, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NotNull Object input)
    {
        return fun.apply((IPLocationConst) input);
    }

    @Override
    public @NotNull Class<?> getInputClass()
    {
        return IPLocationConst.class;
    }
}
