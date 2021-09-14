package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
public class StepExecutorString extends StepExecutor
{
    @ToString.Exclude
    private final Function<String, Boolean> fun;

    public StepExecutorString(IPLogger logger, Function<String, Boolean> fun)
    {
        super(logger);
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "String input");
        return fun.apply((String) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return String.class;
    }
}
