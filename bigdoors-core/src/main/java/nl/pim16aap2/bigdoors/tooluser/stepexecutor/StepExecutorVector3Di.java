package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@AllArgsConstructor
public class StepExecutorVector3Di extends StepExecutor
{
    @NotNull
    private final Function<Vector3DiConst, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NotNull Object input)
    {
        return fun.apply((Vector3DiConst) input);
    }

    @Override
    public @NotNull Class<?> getInputClass()
    {
        return Vector3DiConst.class;
    }
}
