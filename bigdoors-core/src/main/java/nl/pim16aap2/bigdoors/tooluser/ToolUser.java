package nl.pim16aap2.bigdoors.tooluser;

import lombok.Getter;
import lombok.ToString;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import nl.pim16aap2.bigdoors.util.messages.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@ToString
public abstract class ToolUser implements IRestartable
{
    @Getter
    private final @NotNull IPPlayer player;

    /**
     * The {@link Procedure} that this {@link ToolUser} will go through.
     */
    @Getter
    private final @NotNull Procedure procedure;

    /**
     * Checks if this {@link ToolUser} has been shut down or not.
     */
    private final @NotNull AtomicBoolean isShutDown = new AtomicBoolean(false);

    /**
     * Keeps track of whether this {@link ToolUser} is active or not.
     */
    @Getter
    protected boolean active = true;

    /**
     * Keeps track of whether or not the player has the tool or not.
     */
    protected boolean playerHasStick = false;

    protected ToolUser(final @NotNull IPPlayer player)
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
                                              getPlayer().asString(), e);
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
    @Initializer
    protected abstract void init();

    /**
     * Generates the list of {@link IStep}s that together will make up the {@link #procedure}.
     *
     * @return The list of {@link IStep}s that together will make up the {@link #procedure}.
     *
     * @throws InstantiationException When a step's factory is incomplete or otherwise invalid.
     */
    protected abstract @NotNull List<IStep> generateSteps()
        throws InstantiationException;

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
    protected final void giveTool(final @NotNull Message name, final @NotNull Message lore,
                                  final @Nullable Message message)
    {
        BigDoors.get().getPlatform().getBigDoorsToolUtil()
                .giveToPlayer(getPlayer(),
                              BigDoors.get().getPlatform().getMessages().getString(name),
                              BigDoors.get().getPlatform().getMessages().getString(lore));
        playerHasStick = true;

        if (message != null)
            getPlayer().sendMessage(BigDoors.get().getPlatform().getMessages().getString(message));
    }

    /**
     * Removes the BigDoors tool from the player's inventory.
     */
    protected final void removeTool()
    {
        BigDoors.get().getPlatform().getBigDoorsToolUtil().removeTool(getPlayer());
        playerHasStick = false;
    }

    /**
     * Gets the message for the current step.
     *
     * @return The message of the current step if possible. Otherwise, an empty String is returned.
     */
    public @NotNull String getCurrentStepMessage()
    {
        return getProcedure().getMessage();
    }

    /**
     * Prepares the next step. For example by sending the player some instructions about what they should do.
     * <p>
     * This should be used after proceeding to the next step.
     */
    protected void prepareCurrentStep()
    {
        sendMessage();
        if (!getProcedure().waitForUserInput())
            handleInput(null);
    }

    /**
     * See {@link Procedure#skipToStep(IStep)}.
     * <p>
     * After successfully skipping to the target step, the newly-selected step will be prepared. See {@link
     * #prepareCurrentStep()}.
     */
    protected boolean skipToStep(final @NotNull IStep goalStep)
    {
        if (!getProcedure().skipToStep(goalStep))
            return false;
        prepareCurrentStep();
        return true;
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
            return getProcedure().applyStepExecutor(obj);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(e, toString());
            // TODO: Localization
            getPlayer().sendMessage("An error occurred! Please contact a server administrator!");
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
    public boolean handleInput(final @Nullable Object obj)
    {
        BigDoors.get().getPLogger().debug(
            "Handling input: " + obj + " (" + (obj == null ? "null" : obj.getClass().getSimpleName()) + ") for step: " +
                getProcedure().getCurrentStepName() + " in ToolUser: " + this);

        if (!active)
            return false;

        final boolean isLastStep = !getProcedure().hasNextStep();

        if (!applyInput(obj))
            return false;

        if (isLastStep)
        {
            cleanUpProcess();
            return true;
        }

        if (getProcedure().implicitNextStep())
            getProcedure().goToNextStep();

        prepareCurrentStep();
        return true;
    }

    /**
     * Sends the localized message of the current step to the player that owns this object.
     */
    protected void sendMessage()
    {
        @NotNull val message = getProcedure().getMessage();
        if (message.isEmpty())
            BigDoors.get().getPLogger().warn("Missing translation for step: " + getProcedure().getCurrentStepName());
        else
            getPlayer().sendMessage(message);
    }

    /**
     * Gets the {@link IStep} in the {@link #procedure} this {@link ToolUser} is currently at.
     *
     * @return The current {@link IStep} in the {@link #procedure}.
     */
    public @NotNull Optional<IStep> getCurrentStep()
    {
        return Optional.ofNullable(getProcedure().getCurrentStep());
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
    public boolean playerHasAccessToLocation(final @NotNull IPLocation loc)
    {
        final @NotNull Optional<String> result = BigDoors.get().getPlatform().getProtectionCompatManager()
                                                         .canBreakBlock(getPlayer(), loc);

        result.ifPresent(
            compat ->
            {
                if (!compat.isEmpty())
                    getPlayer().sendMessage(BigDoors.get().getPlatform().getMessages()
                                                    .getString(Message.ERROR_NOPERMISSIONFORLOCATION, compat));
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
    public boolean playerHasAccessToCuboid(final @NotNull Cuboid cuboid, final @NotNull IPWorld world)
    {
        final @NotNull Optional<String> result = BigDoors.get().getPlatform().getProtectionCompatManager()
                                                         .canBreakBlocksBetweenLocs(getPlayer(), cuboid.getMin(),
                                                                                    cuboid.getMax(), world);

        result.ifPresent(
            compat ->
            {
                if (!compat.isEmpty())
                    getPlayer().sendMessage(BigDoors.get().getPlatform().getMessages()
                                                    .getString(Message.ERROR_NOPERMISSIONFORLOCATION, compat));
            });
        return result.isEmpty();
    }
}
