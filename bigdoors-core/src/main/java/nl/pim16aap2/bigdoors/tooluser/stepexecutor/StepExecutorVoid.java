package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import com.google.common.flogger.StackSize;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@ToString
@Flogger
public class StepExecutorVoid extends StepExecutor
{
    @ToString.Exclude
    private final Supplier<Boolean> fun;

    public StepExecutorVoid(Supplier<Boolean> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        if (input != null)
            log.atFine().withStackTrace(StackSize.FULL)
               .log("Void input should not have a value. Received %s", input);
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
