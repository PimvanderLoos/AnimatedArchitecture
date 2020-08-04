package nl.pim16aap2.bigdoors.tooluser.step;

import lombok.AllArgsConstructor;
import nl.pim16aap2.bigdoors.tooluser.ToolUser;
import nl.pim16aap2.bigdoors.util.vector.IVector3DiConst;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

@AllArgsConstructor
public class StepVector3Di<T extends ToolUser<T>> extends Step<T>
{
    @NotNull
    private final BiConsumer<T, IVector3DiConst> consumer;

    @Override
    protected void protectedAccept(final @NotNull T toolUser, final @NotNull Object input)
    {
        consumer.accept(toolUser, (IVector3DiConst) input);
    }

    @Override
    public Class<?> getInputClass()
    {
        return IVector3DiConst.class;
    }
}
