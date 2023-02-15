package nl.pim16aap2.bigdoors.core.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.core.api.ILocation;
import nl.pim16aap2.bigdoors.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@ToString
public class StepExecutorLocation extends StepExecutor
{
    @ToString.Exclude
    private final Predicate<ILocation> fun;

    public StepExecutorLocation(Predicate<ILocation> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "Location input");
        return fun.test((ILocation) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return ILocation.class;
    }
}
