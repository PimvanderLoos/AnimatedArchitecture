package nl.pim16aap2.bigdoors.tooluser;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.tooluser.step.StepString;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * Gets the localized message associated with a given {@link Step}. If no message could be found, an empty String is
     * returned.
     *
     * @param step The {@link Step} for which to get the message.
     * @return The localized message for the given {@link Step}.
     */
    protected abstract String getStepMessage(final @NotNull Step<T> step);

    /**
     * Gets the procedure (ordered list of steps) that this {@link ToolUser} has to go through.
     *
     * @return The procedure (ordered list of steps) that this {@link ToolUser} has to go through.
     */
    public abstract List<Step<T>> getProcedure();

    /**
     * Sends the localized message of the current {@link Step} to the player that owns this object.
     *
     * @param step The step to inform the user about.
     */
    protected void sendMessage(final @NotNull Step<T> step)
    {
        String message = getStepMessage(step);
        if (message.isEmpty())
            PLogger.get().warn("Missing translation for step: " + step.getClass().getSimpleName());
        else
            player.sendMessage(message);
    }

    public void handleInput(final @Nullable Object obj)
    {
        {
            System.out.print(" ");
            System.out.print("handleInput! stepIDX = " + stepIDX);
            Optional<Step<T>> current_step = getCurrentStep();
            if (current_step.isPresent())
            {
                System.out.print("Current step is present: " + current_step.get().getClass().getSimpleName());
                if (current_step.get() instanceof StepString)
                {
                    System.out.print("String input to handle: " + (String) obj);
                }
            }
            else
                System.out.print("Current step is not present!");
        }

        getCurrentStep().ifPresent(
            step ->
            {
                try
                {
                    if (!step.apply((T) this, obj))
                        sendMessage(step);
                }
                catch (Exception e)
                {
                    PLogger.get().logException(e);
                }
            });
    }

    /**
     * Handles a confirmation. I.e. input where the fact that there is any input at all is the input itself and
     * therefore doesn't have a value.
     */
    public void handleConfirm()
    {
        System.out.print("CONFIRM!");
        handleInput(null);
    }

    /**
     * Checks if a player is allowed to break the block in a given location.
     * <p>
     * If the player is not allowed to break blocks in the location, a message will be sent to them.
     *
     * @param loc The location to check.
     * @return True if the player is allowed to break the block at the given location.
     */
    protected boolean playerHasAccessToLocation(final @NotNull IPLocation loc)
    {
        // TODO: Implement.
        return true;
    }
}
