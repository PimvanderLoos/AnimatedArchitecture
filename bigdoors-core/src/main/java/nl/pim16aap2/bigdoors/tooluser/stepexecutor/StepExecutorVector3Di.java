package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@AllArgsConstructor
public class StepExecutorVector3Di extends StepExecutor
{
    @NotNull
    private final Function<IVector3DiConst, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NotNull Object input)
    {
        return fun.apply((IVector3DiConst) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return IVector3DiConst.class;
    }
}
