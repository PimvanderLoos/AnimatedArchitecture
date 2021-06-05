package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.ToString;
import nl.pim16aap2.bigdoors.util.Util;
import nl.pim16aap2.bigdoors.util.vector.Vector3DiConst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

@ToString
@AllArgsConstructor
public class StepExecutorVector3Di extends StepExecutor
{
    @ToString.Exclude
    private final @NotNull Function<Vector3DiConst, Boolean> fun;

    @Override
    protected boolean protectedAccept(final @Nullable Object input)
    {
        Util.requireNonNull(input, "Vector input");
        return fun.apply((Vector3DiConst) input);
    }

    @Override
    public @NotNull Class<?> getInputClass()
    {
        return Vector3DiConst.class;
    }
}
