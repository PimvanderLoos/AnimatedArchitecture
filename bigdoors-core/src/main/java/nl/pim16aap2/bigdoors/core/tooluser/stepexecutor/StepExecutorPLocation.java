package nl.pim16aap2.bigdoors.core.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.core.api.IPLocation;
import nl.pim16aap2.bigdoors.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
public class StepExecutorPLocation extends StepExecutor
{
    @ToString.Exclude
    private final Function<IPLocation, Boolean> fun;

    public StepExecutorPLocation(Function<IPLocation, Boolean> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "Location input");
        return fun.apply((IPLocation) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return IPLocation.class;
    }
}
