package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
@AllArgsConstructor
public class StepExecutorPLocation extends StepExecutor
{
    @ToString.Exclude
    private final Function<IPLocation, Boolean> fun;

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
