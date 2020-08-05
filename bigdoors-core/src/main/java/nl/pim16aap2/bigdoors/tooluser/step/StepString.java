package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

@AllArgsConstructor
public class StepString<T extends ToolUser<T>> extends Step<T>
{
    @NotNull
    private final BiFunction<T, String, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @NotNull T toolUser, final @NotNull Object input)
    {
        return fun.apply(toolUser, (String) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return String.class;
    }
}
