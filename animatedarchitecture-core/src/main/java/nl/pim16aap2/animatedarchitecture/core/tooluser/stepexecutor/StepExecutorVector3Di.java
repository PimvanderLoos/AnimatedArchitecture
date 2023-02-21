package nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@ToString
public class StepExecutorVector3Di extends StepExecutor
{
    @ToString.Exclude
    private final Predicate<Vector3Di> fun;

    public StepExecutorVector3Di(Predicate<Vector3Di> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "Vector input");
        return fun.test((Vector3Di) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return Vector3Di.class;
    }
}
