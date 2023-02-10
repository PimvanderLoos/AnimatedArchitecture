package nl.pim16aap2.bigdoors.core.tooluser;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.bigdoors.core.annotations.Initializer;
import nl.pim16aap2.bigdoors.core.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.core.api.IEconomyManager;
import nl.pim16aap2.bigdoors.core.api.ILocation;
import nl.pim16aap2.bigdoors.core.api.IPlayer;
import nl.pim16aap2.bigdoors.core.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.core.api.IWorld;
import nl.pim16aap2.bigdoors.core.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.core.commands.CommandFactory;
import nl.pim16aap2.bigdoors.core.localization.ILocalizer;
import nl.pim16aap2.bigdoors.core.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.core.managers.LimitsManager;
import nl.pim16aap2.bigdoors.core.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.core.structures.StructureBaseBuilder;
import nl.pim16aap2.bigdoors.core.text.TextType;
import nl.pim16aap2.bigdoors.core.tooluser.step.IStep;
import nl.pim16aap2.bigdoors.core.tooluser.step.Step;
import nl.pim16aap2.bigdoors.core.util.Cuboid;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@ToString
@Flogger
public abstract class ToolUser
{
    @Getter
    private final IPlayer player;

    protected final ILocalizer localizer;

    protected final ToolUserManager toolUserManager;

    protected final IProtectionCompatManager protectionCompatManager;

    protected final IBigDoorsToolUtil bigDoorsToolUtil;

    protected final ITextFactory textFactory;

    protected final Step.Factory.IFactory stepFactory;

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

    protected ToolUser(Context context, IPlayer player)
    {
        stepFactory = context.getStepFactory();
        this.player = player;
        localizer = context.getLocalizer();
        toolUserManager = context.getToolUserManager();
        protectionCompatManager = context.getProtectionCompatManager();
        bigDoorsToolUtil = context.getBigDoorsToolUtil();
        textFactory = context.getTextFactory();

        init();

        try
        {
            procedure = new Procedure(generateSteps(), localizer);
        }
        catch (InstantiationException | IndexOutOfBoundsException e)
        {
            throw new RuntimeException("Failed to instantiate procedure for ToolUser for player: " +
                                           getPlayer().asString(), e);
        }

        if (!active)
            return;

        toolUserManager.registerToolUser(this);
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
        toolUserManager.abortToolUser(this);
    }

    public void abort()
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
        bigDoorsToolUtil.giveToPlayer(getPlayer(), localizer.getMessage(nameKey), localizer.getMessage(loreKey));
        playerHasStick = true;

        if (messageKey != null)
            getPlayer().sendMessage(textFactory, TextType.SUCCESS, localizer.getMessage(messageKey));
    }

    /**
     * Removes the BigDoors tool from the player's inventory.
     */
    protected final void removeTool()
    {
        bigDoorsToolUtil.removeTool(getPlayer());
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
        getProcedure().runCurrentStepPreparation();
        sendMessage();
        if (!getProcedure().waitForUserInput())
            handleInput(null);
    }

    /**
     * See {@link Procedure#skipToStep(IStep)}.
     * <p>
     * After successfully skipping to the target step, the newly-selected step will be prepared. See
     * {@link #prepareCurrentStep()}.
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
            log.atSevere().withCause(e).log("Failed to apply input %s to ToolUser %s", obj, this);
            getPlayer().sendMessage(textFactory, TextType.ERROR, localizer.getMessage("constants.error.generic"));
            abort();
            return false;
        }
    }

    @SuppressWarnings("PMD.PrematureDeclaration")
    private boolean handleInput0(@Nullable Object obj)
    {
        log.atFine().log("Handling input: %s (%s) for step: %s in ToolUser: %s.",
                         obj, (obj == null ? "null" : obj.getClass().getSimpleName()),
                         getProcedure().getCurrentStepName(), this);

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
     * Handles user input for the given step.
     *
     * @param obj
     *     The input to handle. What actual type is expected depends on the step.
     * @return True if the input was processed successfully.
     */
    public boolean handleInput(@Nullable Object obj)
    {
        try
        {
            return handleInput0(obj);
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to handle input '%s' for ToolUser '%s'!", obj, this);
            return false;
        }
    }

    /**
     * Sends the localized message of the current step to the player that owns this object.
     */
    protected void sendMessage()
    {
        final var message = getProcedure().getMessage();
        if (message.isEmpty())
            log.atWarning().log("Missing translation for step: %s", getProcedure().getCurrentStepName());
        else
            getPlayer().sendMessage(textFactory, TextType.INFO, message);
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
    public boolean playerHasAccessToLocation(ILocation loc)
    {
        final Optional<String> result = protectionCompatManager.canBreakBlock(getPlayer(), loc);
        result.ifPresent(
            compat ->
            {
                log.atFine().log("Blocked access to cuboid %s for player %s! Reason: %s",
                                 loc, getPlayer(), compat);
                getPlayer().sendMessage(textFactory, TextType.ERROR,
                                        localizer.getMessage("tool_user.base.error.no_permission_for_location"));
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
    public boolean playerHasAccessToCuboid(Cuboid cuboid, IWorld world)
    {
        final Optional<String> result = protectionCompatManager.canBreakBlocksBetweenLocs(getPlayer(), cuboid.getMin(),
                                                                                          cuboid.getMax(), world);
        result.ifPresent(
            compat ->
            {
                log.atFine().log("Blocked access to cuboid %s for player %s in world %s. Reason: %s",
                                 cuboid, getPlayer(), world, compat);
                getPlayer().sendMessage(textFactory, TextType.ERROR,
                                        localizer.getMessage("tool_user.base.error.no_permission_for_location"));
            });
        return result.isEmpty();
    }

    @Getter
    public static final class Context
    {
        private final StructureBaseBuilder structureBaseBuilder;
        private final ILocalizer localizer;
        private final ITextFactory textFactory;
        private final ToolUserManager toolUserManager;
        private final DatabaseManager databaseManager;
        private final LimitsManager limitsManager;
        private final IEconomyManager economyManager;
        private final IProtectionCompatManager protectionCompatManager;
        private final IBigDoorsToolUtil bigDoorsToolUtil;
        private final CommandFactory commandFactory;
        private final Step.Factory.IFactory stepFactory;

        @Inject
        public Context(
            StructureBaseBuilder structureBaseBuilder, ILocalizer localizer, ITextFactory textFactory,
            ToolUserManager toolUserManager, DatabaseManager databaseManager, LimitsManager limitsManager,
            IEconomyManager economyManager, IProtectionCompatManager protectionCompatManager,
            IBigDoorsToolUtil bigDoorsToolUtil, CommandFactory commandFactory,
            Step.Factory.IFactory stepFactory)
        {
            this.structureBaseBuilder = structureBaseBuilder;
            this.localizer = localizer;
            this.toolUserManager = toolUserManager;
            this.databaseManager = databaseManager;
            this.limitsManager = limitsManager;
            this.economyManager = economyManager;
            this.protectionCompatManager = protectionCompatManager;
            this.bigDoorsToolUtil = bigDoorsToolUtil;
            this.commandFactory = commandFactory;
            this.textFactory = textFactory;
            this.stepFactory = stepFactory;
        }
    }
}
