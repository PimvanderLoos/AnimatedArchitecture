package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.util.MovementDirection;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
public class StepExecutorOpenDirection extends StepExecutor
{
    @ToString.Exclude
    private final Function<MovementDirection, Boolean> fun;

    public StepExecutorOpenDirection(Function<MovementDirection, Boolean> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "MovementDirection input");
        return fun.apply((MovementDirection) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return MovementDirection.class;
    }
}
