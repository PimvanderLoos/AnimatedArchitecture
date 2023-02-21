package nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@ToString
public class StepExecutorBoolean extends StepExecutor
{
    @ToString.Exclude
    private final Predicate<Boolean> fun;

    public StepExecutorBoolean(Predicate<Boolean> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "Boolean input");
        return fun.test((Boolean) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return Boolean.class;
    }
}
