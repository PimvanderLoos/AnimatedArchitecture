package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
public class StepExecutorBoolean extends StepExecutor
{
    @ToString.Exclude
    private final Function<Boolean, Boolean> fun;

    public StepExecutorBoolean(IPLogger logger, Function<Boolean, Boolean> fun)
    {
        super(logger);
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "Boolean input");
        return fun.apply((Boolean) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return Boolean.class;
    }
}
