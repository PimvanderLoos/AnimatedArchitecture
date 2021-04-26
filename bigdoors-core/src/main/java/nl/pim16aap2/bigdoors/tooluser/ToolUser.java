package nl.pim16aap2.bigdoors.tooluser;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IPLocationConst;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@ToString
public abstract class ToolUser implements IRestartable
{
    @Getter
    protected final @NonNull IPPlayer player;
    @ToString.Exclude
    protected final @NonNull Messages messages = BigDoors.get().getPlatform().getMessages();
    protected final @NonNull Procedure procedure;

    /**
     * Checks if this {@link ToolUser} has been shut down or not.
     */
    private final @NonNull AtomicBoolean isShutDown = new AtomicBoolean(false);

    /**
     * Keeps track of whether this {@link ToolUser} is active or not.
     */
    @Getter
    protected boolean active = true;

    protected boolean playerHasStick = false;

    protected ToolUser(final @NonNull IPPlayer player)
    {
        this.player = player;
        init();

        try
        {
            procedure = new Procedure(this, generateSteps());
        }
        catch (InstantiationException | IndexOutOfBoundsException e)
        {
            val ex = new RuntimeException("Failed to instantiate procedure for ToolUser for player: " +
                                              player.asString(), e);
            BigDoors.get().getPLogger().logThrowableSilently(ex);
            throw ex;
        }

        if (!active)
            return;

        BigDoors.get().getToolUserManager().registerToolUser(this);
    }

    /**
     * Basic initialization executed at the start of the constructor.
     */
    protected abstract void init();

    /**
     * Generates the list of {@link IStep}s that together will make up the {@link #procedure}.
     *
     * @return The list of {@link IStep}s that together will make up the {@link #procedure}.
     *
     * @throws InstantiationException When a step's factory is incomplete or otherwise invalid.
     */
    protected abstract @NonNull List<IStep> generateSteps()
        throws InstantiationException;

    /**
     * Gets the {@link Procedure} that this {@link ToolUser} will go through.
     *
     * @return The {@link Procedure} for this {@link ToolUser}.
     */
    public @NonNull Procedure getProcedure()
    {
        return procedure;
    }

    /**
     * Takes care of the final part of the process. This unregisters this {@link ToolUser} and removes the tool from the
     * player's inventory (if they still have it).
     */
    protected final void cleanUpProcess()
    {
        if (isShutDown.getAndSet(true))
            return;
        removeTool();
        active = false;
        BigDoors.get().getToolUserManager().abortToolUser(this);
    }

    @Override
    public void restart()
    {
        cleanUpProcess();
    }

    @Override
    public void shutdown()
    {
        cleanUpProcess();
    }

    /**
     * Adds the BigDoors tool from the player's inventory.
     *
     * @param name    The name of the tool.
     * @param lore    The lore of the tool.
     * @param message The message to send to the player after giving them the tool.
     */
    protected final void giveTool(final @NonNull Message name, final @NonNull Message lore,
                                  final @Nullable Message message)
    {
        BigDoors.get().getPlatform().getBigDoorsToolUtil()
                .giveToPlayer(player, messages.getString(name), messages.getString(lore));
        playerHasStick = true;

        if (message != null)
            player.sendMessage(messages.getString(message));
    }

    /**
     * Removes the BigDoors tool from the player's inventory.
     */
    protected final void removeTool()
    {
        BigDoors.get().getPlatform().getBigDoorsToolUtil().removeTool(player);
        playerHasStick = false;
    }

    /**
     * Gets the message for the current step.
     *
     * @return The message of the current step if possible. Otherwise, an empty String is returned.
     */
    public @NonNull String getCurrentStepMessage()
    {
        return procedure.getMessage();
    }

    /**
     * Prepares the next step. For example by sending the player some instructions about what they should do.
     * <p>
     * This should be used after proceeding to the next step.
     */
    protected void prepareCurrentStep()
    {
        sendMessage();

        if (!procedure.waitForUserInput() &&
            applyInput(null) &&
            procedure.implicitNextStep())
        {
            procedure.goToNextStep();
            prepareCurrentStep();
        }
    }

    /**
     * Applies an input object to the current step in the {@link #procedure}.
     *
     * @param obj The object to apply.
     * @return The result of running the step executor on the provided input.
     */
    private boolean applyInput(final @Nullable Object obj)
    {
        try
        {
            return procedure.applyStepExecutor(obj);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(e);
            // TODO: Localization
            player.sendMessage("An error occurred! Please contact a server administrator!");
            shutdown();
            return false;
        }
    }

    /**
     * Handles user input for the given step.
     *
     * @param obj The input to handle. What actual type is expected depends on the step.
     * @return True if the input was processed successfully.
     */
    public boolean handleInput(final @NonNull Object obj)
    {
        BigDoors.get().getPLogger().debug(
            "Handling input: " + obj + " (" + obj.getClass().getSimpleName() + ") for step: " +
                procedure.getCurrentStepName() + " in ToolUser: " + this);

        if (!active)
            return false;

        if (!applyInput(obj))
            return false;

        if (procedure.implicitNextStep())
            procedure.goToNextStep();

        prepareCurrentStep();
        return true;
    }

    /**
     * Sends the localized message of the current step to the player that owns this object.
     */
    protected void sendMessage()
    {
        @NonNull val message = procedure.getMessage();
        if (message.isEmpty())
            BigDoors.get().getPLogger().warn("Missing translation for step: " + procedure.getCurrentStepName());
        else
            player.sendMessage(message);
    }

    /**
     * Gets the {@link IStep} in the {@link #procedure} this {@link ToolUser} is currently at.
     *
     * @return The current {@link IStep} in the {@link #procedure}.
     */
    public @NonNull Optional<IStep> getCurrentStep()
    {
        return Optional.ofNullable(procedure.getCurrentStep());
    }

    /**
     * Checks if a player is allowed to break the block in a given location.
     * <p>
     * If the player is not allowed to break blocks in the location, a message will be sent to them (provided the name
     * of the compat isn't empty).
     *
     * @param loc The location to check.
     * @return True if the player is allowed to break the block at the given location.
     */
    protected boolean playerHasAccessToLocation(final @NonNull IPLocationConst loc)
    {
        final @NonNull Optional<String> result = BigDoors.get().getPlatform().getProtectionCompatManager()
                                                         .canBreakBlock(player, loc);

        result.ifPresent(
            compat ->
            {
                if (!compat.isEmpty())
                    player.sendMessage(messages.getString(Message.ERROR_NOPERMISSIONFORLOCATION, compat));
            });
        return result.isEmpty();
    }

    /**
     * Checks if a player is allowed to break all blocks in a given cuboid.
     * <p>
     * If the player is not allowed to break one or more blocks in the cuboid, a message will be sent to them. (provided
     * the name of the compat isn't empty).
     *
     * @param cuboid The cuboid to check.
     * @param world  The world to check in.
     * @return True if the player is allowed to break all blocks inside the cuboid.
     */
    protected boolean playerHasAccessToCuboid(final @NonNull Cuboid cuboid, final @NonNull IPWorld world)
    {
        final @NonNull Optional<String> result = BigDoors.get().getPlatform().getProtectionCompatManager()
                                                         .canBreakBlocksBetweenLocs(player, cuboid.getMin(),
                                                                                    cuboid.getMax(), world);

        result.ifPresent(
            compat ->
            {
                if (!compat.isEmpty())
                    player.sendMessage(messages.getString(Message.ERROR_NOPERMISSIONFORLOCATION, compat));
            });
        return result.isEmpty();
    }
}
