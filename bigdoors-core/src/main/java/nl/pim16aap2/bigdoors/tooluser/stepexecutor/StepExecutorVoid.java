package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@AllArgsConstructor
public class StepExecutorVoid extends StepExecutor
{
    private final @NonNull Supplier<Boolean> fun;

    @Override
    protected boolean protectedAccept(final @Nullable Object input)
    {
        return fun.get();
    }

    @Override
    public boolean validInput(final @Nullable Object obj)
    {
        return obj == null;
    }

    @Override
    public @NonNull Class<?> getInputClass()
    {
        return Object.class;
    }
}
