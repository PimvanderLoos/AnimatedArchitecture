package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;

import java.util.function.Function;

@ToString
@AllArgsConstructor
public class StepExecutorVector3Di extends StepExecutor
{
    @ToString.Exclude
    private final @NonNull Function<Vector3DiConst, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NonNull Object input)
    {
        return fun.apply((Vector3DiConst) input);
    }

    @Override
    public @NonNull Class<?> getInputClass()
    {
        return Vector3DiConst.class;
    }
}
