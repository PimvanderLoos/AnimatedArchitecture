package nl.pim16aap2.bigdoors.tooluser;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public abstract class ToolUser<T extends ToolUser<T>>
{
    protected int stepIDX = 0;
    @Getter
    @NonNull
    protected final IPPlayer player;
    @NotNull
    protected final Messages messages = BigDoors.get().getPlatform().getMessages();

    protected ToolUser(final @NotNull IPPlayer pPlayer)
    {
        player = pPlayer;
        ToolUserManager.get().registerToolUser(this);
    }

    protected Optional<Step<T>> getCurrentStep()
    {
        List<Step<T>> procedure = getProcedure();
        if (stepIDX > procedure.size() || stepIDX < 0)
        {
            PLogger.get().logException(
                new ArrayIndexOutOfBoundsException("Tried to get step #" + stepIDX + ", but this " +
                                                       "procedure only has " + procedure.size() + " steps!"));
            return Optional.empty();
        }
        return Optional.of(procedure.get(stepIDX));
    }

    /**
     * Gets the procedure (ordered list of steps) that this {@link ToolUser} has to go through.
     *
     * @return The procedure (ordered list of steps) that this {@link ToolUser} has to go through.
     */
    public abstract List<Step<T>> getProcedure();

    public void handleInput(final @NotNull Object obj)
    {
        getCurrentStep().ifPresent(step -> step.accept((T) this, obj));
    }
}
