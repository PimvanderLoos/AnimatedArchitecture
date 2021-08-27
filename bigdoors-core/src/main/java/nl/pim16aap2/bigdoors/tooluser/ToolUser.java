package nl.pim16aap2.bigdoors.tooluser;

import lombok.Getter;
import lombok.ToString;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.annotations.Initializer;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.restartable.IRestartable;
import nl.pim16aap2.bigdoors.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.util.Cuboid;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

@ToString
public abstract class ToolUser implements IRestartable
{
    @Getter
    private final IPPlayer player;

    /**
     * The {@link Procedure} that this {@link ToolUser} will go through.
     */
    @Getter
    private final Procedure procedure;

    /**
     * Checks if this {@link ToolUser} has been shut down or not.
     */
    private final AtomicBoolean isShutDown = new AtomicBoolean(false);

    /**
     * Keeps track of whether this {@link ToolUser} is active or not.
     */
    @Getter
    protected boolean active = true;

    /**
     * Keeps track of whether the player has the tool or not.
     */
    protected boolean playerHasStick = false;

    protected ToolUser(IPPlayer player)
    {
        this.player = player;
        init();

        try
        {
            procedure = new Procedure(generateSteps());
        }
        catch (InstantiationException | IndexOutOfBoundsException e)
        {
            final var ex = new RuntimeException("Failed to instantiate procedure for ToolUser for player: " +
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
     * @throws InstantiationException
     *     When a step's factory is incomplete or otherwise invalid.
     */
    protected abstract List<IStep> generateSteps()
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
     * @param nameKey
     *     The localization key of the name of the tool.
     * @param loreKey
     *     The localization key of the lore of the tool.
     * @param messageKey
     *     The localization key of the message to send to the player after giving them the tool.
     */
    protected final void giveTool(String nameKey, String loreKey, @Nullable String messageKey)
    {
        BigDoors.get().getPlatform().getBigDoorsToolUtil()
                .giveToPlayer(getPlayer(),
                              BigDoors.get().getLocalizer().getMessage(nameKey),
                              BigDoors.get().getLocalizer().getMessage(loreKey));
        playerHasStick = true;

        if (messageKey != null)
            getPlayer().sendMessage(BigDoors.get().getLocalizer().getMessage(messageKey));
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
    @SuppressWarnings("unused")
    public String getCurrentStepMessage()
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
    @SuppressWarnings("unused")
    protected boolean skipToStep(IStep goalStep)
    {
        if (!getProcedure().skipToStep(goalStep))
            return false;
        prepareCurrentStep();
        return true;
    }

    /**
     * Applies an input object to the current step in the {@link #procedure}.
     *
     * @param obj
     *     The object to apply.
     * @return The result of running the step executor on the provided input.
     */
    private boolean applyInput(@Nullable Object obj)
    {
        try
        {
            return getProcedure().applyStepExecutor(obj);
        }
        catch (Exception e)
        {
            BigDoors.get().getPLogger().logThrowable(e, toString());
            getPlayer().sendMessage(BigDoors.get().getLocalizer().getMessage("constants.error.generic"));
            shutdown();
            return false;
        }
    }

    /**
     * Handles user input for the given step.
     *
     * @param obj
     *     The input to handle. What actual type is expected depends on the step.
     * @return True if the input was processed successfully.
     */
    @SuppressWarnings("PMD.PrematureDeclaration")
    public boolean handleInput(@Nullable Object obj)
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
        final var message = getProcedure().getMessage();
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
    public Optional<IStep> getCurrentStep()
    {
        return Optional.ofNullable(getProcedure().getCurrentStep());
    }

    /**
     * Checks if a player is allowed to break the block in a given location.
     * <p>
     * If the player is not allowed to break blocks in the location, a message will be sent to them (provided the name
     * of the compat isn't empty).
     *
     * @param loc
     *     The location to check.
     * @return True if the player is allowed to break the block at the given location.
     */
    public boolean playerHasAccessToLocation(IPLocation loc)
    {
        final Optional<String> result = BigDoors.get().getPlatform().getProtectionCompatManager()
                                                .canBreakBlock(getPlayer(), loc);
        result.ifPresent(
            compat ->
            {
                BigDoors.get().getPLogger().logMessage(Level.FINE,
                                                       "Blocked access to cuboid " + loc + " for player " +
                                                           getPlayer() + ". Reason: " + compat);
                getPlayer().sendMessage(BigDoors.get().getLocalizer()
                                                .getMessage("tool_user.base.error.no_permission_for_location"));
            });
        return result.isEmpty();
    }

    /**
     * Checks if a player is allowed to break all blocks in a given cuboid.
     * <p>
     * If the player is not allowed to break one or more blocks in the cuboid, a message will be sent to them. (provided
     * the name of the compat isn't empty).
     *
     * @param cuboid
     *     The cuboid to check.
     * @param world
     *     The world to check in.
     * @return True if the player is allowed to break all blocks inside the cuboid.
     */
    public boolean playerHasAccessToCuboid(Cuboid cuboid, IPWorld world)
    {
        final Optional<String> result = BigDoors.get().getPlatform().getProtectionCompatManager()
                                                .canBreakBlocksBetweenLocs(getPlayer(), cuboid.getMin(),
                                                                           cuboid.getMax(), world);
        result.ifPresent(
            compat ->
            {
                BigDoors.get().getPLogger().logMessage(Level.FINE,
                                                       "Blocked access to cuboid " + cuboid + " for player " +
                                                           getPlayer() + " in world " + world + ". Reason: " +
                                                           compat);
                getPlayer().sendMessage(BigDoors.get().getLocalizer()
                                                .getMessage("tool_user.base.error.no_permission_for_location"));
            });
        return result.isEmpty();
    }
}
