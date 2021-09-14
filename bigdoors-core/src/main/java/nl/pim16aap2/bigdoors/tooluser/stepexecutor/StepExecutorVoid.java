package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import java.util.logging.Level;

@ToString
public class StepExecutorVoid extends StepExecutor
{
    @ToString.Exclude
    private final Supplier<Boolean> fun;

    public StepExecutorVoid(IPLogger logger, Supplier<Boolean> fun)
    {
        super(logger);
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        if (input != null)
            logger.dumpStackTrace(Level.FINE, "Void input should not have a value. Received " + input);
        return fun.get();
    }

    @Override
    public boolean validInput(@Nullable Object obj)
    {
        return obj == null;
    }

    @Override
    public Class<?> getInputClass()
    {
        return Object.class;
    }
}
