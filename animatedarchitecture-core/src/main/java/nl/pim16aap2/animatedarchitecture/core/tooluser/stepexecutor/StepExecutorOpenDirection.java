package nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor;

import lombok.ToString;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

@ToString
public class StepExecutorOpenDirection extends StepExecutor
{
    @ToString.Exclude
    private final Predicate<MovementDirection> fun;

    public StepExecutorOpenDirection(Predicate<MovementDirection> fun)
    {
        this.fun = fun;
    }

    @Override
    protected boolean protectedAccept(@Nullable Object input)
    {
        Util.requireNonNull(input, "MovementDirection input");
        return fun.test((MovementDirection) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return MovementDirection.class;
    }
}
