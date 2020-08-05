package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

@AllArgsConstructor
public class StepPLocation<T extends ToolUser<T>> extends Step<T>
{
    @NotNull
    private final BiFunction<T, IPLocationConst, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NotNull T toolUser, final @NotNull Object input)
    {
        return fun.apply(toolUser, (IPLocationConst) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return IPLocationConst.class;
    }
}
