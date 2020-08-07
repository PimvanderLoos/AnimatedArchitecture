package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@AllArgsConstructor
public class StepPLocation extends Step
{
    @NotNull
    private final Function<IPLocationConst, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NotNull Object input)
    {
        return fun.apply((IPLocationConst) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return IPLocationConst.class;
    }
}
