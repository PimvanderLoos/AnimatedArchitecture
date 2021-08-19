package nl.pim16aap2.bigdoors.tooluser.stepexecutor;

import lombok.AllArgsConstructor;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import java.util.logging.Level;

@ToString
@AllArgsConstructor
public class StepExecutorVoid extends StepExecutor
{
    @ToString.Exclude
    private final Supplier<Boolean> fun;

    @Override
    protected boolean protectedAccept(final @Nullable Object input)
    {
        if (input != null)
            BigDoors.get().getPLogger()
                    .dumpStackTrace(Level.FINE, "Void input should not have a value. Received " + input);
        return fun.get();
    }

    @Override
    public boolean validInput(final @Nullable Object obj)
    {
        return obj == null;
    }

    @Override
    public Class<?> getInputClass()
    {
        return Object.class;
    }
}
