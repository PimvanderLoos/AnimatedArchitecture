package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
public class StepExecutorVector3Di extends StepExecutor
{
    @ToString.Exclude
    private final Function<Vector3Di, Boolean> fun;

    public StepExecutorVector3Di(Function<Vector3Di, Boolean> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "Vector input");
        return fun.apply((Vector3Di) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return Vector3Di.class;
    }
}
