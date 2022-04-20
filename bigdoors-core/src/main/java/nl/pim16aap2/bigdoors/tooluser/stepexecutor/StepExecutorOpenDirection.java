package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
public class StepExecutorOpenDirection extends StepExecutor
{
    @ToString.Exclude
    private final Function<RotateDirection, Boolean> fun;

    public StepExecutorOpenDirection(Function<RotateDirection, Boolean> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "RotateDirection input");
        return fun.apply((RotateDirection) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return RotateDirection.class;
    }
}
