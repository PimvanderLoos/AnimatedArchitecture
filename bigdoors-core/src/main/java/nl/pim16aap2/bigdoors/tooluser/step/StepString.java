package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

@AllArgsConstructor
public class StepString<T extends ToolUser<T>> extends Step<T>
{
    @NotNull
    private final BiConsumer<T, String> consumer;

    @Override
    protected void protectedAccept(final @NotNull T toolUser, final @NotNull Object input)
    {
        consumer.accept(toolUser, (String) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return String.class;
    }
}
