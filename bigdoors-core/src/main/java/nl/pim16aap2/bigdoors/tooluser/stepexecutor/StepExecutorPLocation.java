package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.api.IPLocationConst;

import java.util.function.Function;

@ToString
@AllArgsConstructor
public class StepExecutorPLocation extends StepExecutor
{
    @ToString.Exclude
    private final @NonNull Function<IPLocationConst, Boolean> fun;

    @Override
    protected boolean protectedAccept(@NonNull Object input)
    {
        return fun.apply((IPLocationConst) input);
    }

    @Override
    public @NonNull Class<?> getInputClass()
    {
        return IPLocationConst.class;
    }
}
