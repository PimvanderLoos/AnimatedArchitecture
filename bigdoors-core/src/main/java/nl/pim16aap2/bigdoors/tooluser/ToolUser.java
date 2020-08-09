package nl.pim16aap2.bigdoors.tooluser;

import lombok.Getter;
import lombok.NonNull;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IRestartable;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.util.PLogger;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class ToolUser implements IRestartable
{
    protected int stepIDX = 0;
    @Getter
    @NonNull
    protected final IPPlayer player;
    @NotNull
    protected final Messages messages = BigDoors.get().getPlatform().getMessages();

    @NotNull
    protected final List<Step> procedure;

    protected ToolUser(final @NotNull IPPlayer pPlayer)
    {
        player = pPlayer;
        procedure = Collections.unmodifiableList(constructProcedure());
        ToolUserManager.get().registerToolUser(this);
    }

    /**
     * Takes care of the final part of the process. This unregisters this {@link ToolUser} and removes the tool from the
     * player's inventory (if they still have it).
     */
    protected final void completeProcess()
    {
        ToolUserManager.get().removeToolUser(this);
        removeTool();
    }

    @Override
    public void restart()
    {
        completeProcess();
    }

    @Override
    public void shutdown()
    {
        completeProcess();
    }

    /**
     * Adds the BigDoors tool from the player's inventory.
     *
     * @param name    The name of the tool.
     * @param lore    The lore of the tool.
     * @param message The message to send to the player after giving them the tool.
     */
    protected final void giveTool(final @NotNull Message name, final @NotNull Message lore,
                                  final @Nullable Message message)
    {
        BigDoors.get().getPlatform().getBigDoorsToolUtil()
                .giveToPlayer(player, messages.getString(name), messages.getString(lore));

        if (message != null)
            player.sendMessage(messages.getString(message));
    }

    /**
     * Adds the BigDoors tool from the player's inventory.
     *
     * @param name The name of the tool.
     * @param lore The lore of the tool.
     */
    protected final void giveTool(final @NotNull Message name, final @NotNull Message lore)
    {
        giveTool(name, lore, null);
    }

    /**
     * Removes the BigDoors tool from the player's inventory.
     */
    protected final void removeTool()
    {
        BigDoors.get().getPlatform().getBigDoorsToolUtil().removeTool(player);
    }

    @NotNull
    protected final Optional<Step> getStep(final int step)
    {
        if (step > procedure.size() || step < 0)
        {
            PLogger.get().logException(
                new ArrayIndexOutOfBoundsException("Tried to get step #" + step + ", but this " +
                                                       "procedure only has " + procedure.size() + " steps!"));
            return Optional.empty();
        }
        return Optional.of(procedure.get(step));
    }

    @NotNull
    public final Optional<Step> getCurrentStep()
    {
        return getStep(stepIDX);
    }

    /**
     * Gets the localized message associated with a given {@link Step}. If no message could be found, an empty String is
     * returned.
     *
     * @param step The {@link Step} for which to get the message.
     * @return The localized message for the given {@link Step}.
     */
    @NotNull
    public abstract String getStepMessage(final @NotNull Step step);

    /**
     * Gets the procedure (ordered list of steps) that this {@link ToolUser} has to go through. Note that this is an
     * UnmodifiableList!
     *
     * @return The procedure (ordered list of steps) that this {@link ToolUser} has to go through.
     */
    @NotNull
    public final List<Step> getProcedure()
    {
        return procedure;
    }

    /**
     * Constructs the procedure (ordered list of steps) that this {@link ToolUser} has to go through.
     *
     * @return The procedure (ordered list of steps) that this {@link ToolUser} has to go through.
     */
    @NotNull
    protected abstract List<Step> constructProcedure();

    /**
     * Prepares the next step. For example by sending the player some instructions about what they should do.
     */
    protected abstract void prepareNextStep();

    /**
     * Sends the localized message of the current {@link Step} to the player that owns this object.
     *
     * @param step The step to inform the user about.
     */
    protected void sendMessage(final @NotNull Step step)
    {
        final @NotNull String message = getStepMessage(step);
        if (message.isEmpty())
            PLogger.get().warn("Missing translation for step: " + step.getClass().getSimpleName());
        else
            player.sendMessage(message);
    }

    public boolean handleInput(final @Nullable Object obj)
    {
        return getCurrentStep().map(
            step ->
            {
                try
                {
                    if (step.apply(obj))
                    {
                        prepareNextStep();
                        return true;
                    }
                    sendMessage(step);
                }
                catch (Exception e)
                {
                    PLogger.get().logException(e);
                }
                return false;
            }).orElse(false);
    }

    /**
     * Handles a confirmation. I.e. input where the fact that there is any input at all is the input itself and
     * therefore doesn't have a value.
     */
    public boolean handleConfirm()
    {
        return handleInput(null);
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
