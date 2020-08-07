package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@AllArgsConstructor
public class StepVector3Di extends Step
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
