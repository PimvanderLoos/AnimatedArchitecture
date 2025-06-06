package nl.pim16aap2.animatedarchitecture.core.tooluser.creator;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.ExtensionMethod;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.animation.AnimationType;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionCause;
import nl.pim16aap2.animatedarchitecture.core.events.StructureActionType;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.LimitsManager;
import nl.pim16aap2.animatedarchitecture.core.structures.IStructureComponent;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureID;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.PropertyContainer;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Procedure;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.AsyncStepExecutor;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorBoolean;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorLocation;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorOpenDirection;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorString;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorVoid;
import nl.pim16aap2.animatedarchitecture.core.util.CompletableFutureExtensions;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents a specialization of the {@link ToolUser} that is used for creating new {@link Structure}s.
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Flogger
@ExtensionMethod(CompletableFutureExtensions.class)
public abstract class Creator extends ToolUser
{
    protected final LimitsManager limitsManager;

    protected final StructureBuilder structureBuilder;

    protected final DatabaseManager databaseManager;

    protected final IEconomyManager economyManager;

    protected final CommandFactory commandFactory;

    private final @Nullable StructureAnimationRequestBuilder structureAnimationRequestBuilder;

    private final StructureActivityManager structureActivityManager;

    protected final StructureID structureID = StructureID.getUnregisteredID();

    /**
     * The {@link PropertyContainer} that is used to manage the properties of the structure.
     */
    @GuardedBy("this")
    private final PropertyContainer propertyContainer;

    /**
     * The name of the structure that is to be created.
     */
    @ToString.Include
    @GuardedBy("this")
    private @Nullable String name;

    /**
     * The cuboid that defines the location and dimensions of the structure.
     * <p>
     * This region is defined by {@link #firstPos} and the second position selected by the user.
     */
    @ToString.Include
    @GuardedBy("this")
    private @Nullable Cuboid cuboid;

    /**
     * The first point that was selected in the process.
     * <p>
     * Once a second point has been selected, these two are used to construct the {@link #cuboid}.
     */
    @ToString.Include
    @GuardedBy("this")
    private @Nullable Vector3Di firstPos;

    /**
     * The powerblock selected by the user.
     */
    @ToString.Include
    @GuardedBy("this")
    private @Nullable Vector3Di powerblock;

    /**
     * The opening direction selected by the user.
     */
    @ToString.Include
    @GuardedBy("this")
    private @Nullable MovementDirection movementDirection;

    /**
     * The {@link IWorld} this structure is created in.
     */
    @ToString.Include
    @GuardedBy("this")
    private @Nullable IWorld world;

    /**
     * Whether the structure is created in the locked (true) or unlocked (false) state.
     */
    @ToString.Include
    @GuardedBy("this")
    private boolean isLocked = false;

    /**
     * Whether the process is in a state where is may be updated outside the normal execution order.
     */
    @ToString.Include
    @GuardedBy("this")
    private boolean processIsUpdatable = false;

    /**
     * Factory for the {@link Step} that provides the name.
     */
    protected final Step.Factory factoryProvideName;

    /**
     * Factory for the {@link Step} that provides the first position of the area of the structure.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected final Step.Factory factoryProvideFirstPos;

    /**
     * Factory for the {@link Step} that provides the second position of the area of the structure, thus completing the
     * {@link Cuboid}.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected final Step.Factory factoryProvideSecondPos;

    /**
     * Factory for the {@link Step} that provides the position of the structure's rotation point.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected final Step.Factory factoryProvideRotationPointPos;

    /**
     * Factory for the {@link Step} that provides the position of the structure's power block.
     */
    protected final Step.Factory factoryProvidePowerBlockPos;

    /**
     * Factory for the {@link Step} that provides the open status of the structure.
     */
    protected final Step.Factory factoryProvideOpenStatus;

    /**
     * Factory for the {@link Step} that provides the open direction of the structure.
     */
    protected final Step.Factory factoryProvideOpenDir;

    /**
     * Factory for the {@link Step} that allows the player to confirm or reject the price of the structure.
     */
    protected final Step.Factory factoryConfirmPrice;

    /**
     * Factory for the {@link Step} that allows players to review the created structure
     */
    protected final Step.Factory factoryReviewResult;

    /**
     * Factory for the {@link Step} that completes this process.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected final Step.Factory factoryCompleteProcess;

    /**
     * The type of structure this creator will create.
     *
     * @return The type of structure that will be created.
     */
    @Getter
    protected final StructureType structureType;

    protected Creator(ToolUser.Context context, StructureType structureType, IPlayer player, @Nullable String name)
    {
        super(context, player);

        this.structureType = structureType;
        this.propertyContainer = new PropertyContainer();

        this.structureAnimationRequestBuilder = context.getStructureAnimationRequestBuilder();
        this.structureActivityManager = context.getStructureActivityManager();
        this.limitsManager = context.getLimitsManager();
        this.structureBuilder = context.getStructureBuilder();
        this.databaseManager = context.getDatabaseManager();
        this.economyManager = context.getEconomyManager();
        this.commandFactory = context.getCommandFactory();

        this.name = name;

        player.sendInfo(
            "creator.base.init",
            arg -> arg.clickable(
                "/AnimatedArchitecture cancel", TextType.CLICKABLE_REFUSE, "/AnimatedArchitecture cancel")
        );

        factoryProvideName = stepFactory
            .stepName(localizer, "SET_NAME")
            .stepExecutor(new StepExecutorString(this::completeNamingStep))
            .propertyName(localizer.getMessage("creator.base.property.type"))
            .propertyValueSupplier(this::getName)
            .updatable(true)
            .textSupplier(text -> text.append(
                localizer.getMessage("creator.base.give_name"), TextType.SUCCESS, getStructureArg()));

        factoryProvideFirstPos = stepFactory
            .stepName(localizer, "SET_FIRST_POS")
            .stepExecutor(new AsyncStepExecutor<>(ILocation.class, this::provideFirstPos));

        factoryProvideSecondPos = stepFactory
            .stepName(localizer, "SET_SECOND_POS")
            .propertyName(localizer.getMessage("creator.base.property.cuboid"))
            .propertyValueSupplier(() ->
            {
                final @Nullable Cuboid cuboid0 = getCuboid();
                return cuboid0 == null ? "[]" :
                    String.format("[%s; %s]", formatVector(cuboid0.getMin()), formatVector(cuboid0.getMax()));
            })
            .stepExecutor(new AsyncStepExecutor<>(ILocation.class, this::provideSecondPos));

        factoryProvideRotationPointPos = stepFactory
            .stepName(localizer, "SET_ROTATION_POINT")
            .propertyName(localizer.getMessage("creator.base.property.rotation_point"))
            .propertyValueSupplier(() -> formatVector(getRequiredProperty(Property.ROTATION_POINT)))
            .updatable(true)
            .stepExecutor(new StepExecutorLocation(this::completeSetRotationPointStep));

        factoryProvidePowerBlockPos = stepFactory
            .stepName(localizer, "SET_POWER_BLOCK_POS")
            .messageKey("creator.base.set_power_block")
            .propertyName(localizer.getMessage("creator.base.property.power_block_position"))
            .propertyValueSupplier(() -> formatVector(getPowerBlock()))
            .updatable(true)
            .stepExecutor(new AsyncStepExecutor<>(ILocation.class, this::completeSetPowerBlockStep));

        factoryProvideOpenStatus = stepFactory
            .stepName(localizer, "SET_OPEN_STATUS")
            .stepExecutor(new StepExecutorBoolean(this::completeSetOpenStatusStep))
            .stepPreparation(this::prepareSetOpenStatus)
            .propertyName(localizer.getMessage("creator.base.property.open_status"))
            .propertyValueSupplier(
                () -> getRequiredProperty(Property.OPEN_STATUS) ?
                    localizer.getMessage("constants.open_status.open") :
                    localizer.getMessage("constants.open_status.closed"))
            .updatable(true)
            .textSupplier(this::setOpenStatusTextSupplier);

        factoryProvideOpenDir = stepFactory
            .stepName(localizer, "SET_OPEN_DIRECTION")
            .stepExecutor(new StepExecutorOpenDirection(this::completeSetOpenDirStep))
            .stepPreparation(this::prepareSetOpenDirection)
            .propertyName(localizer.getMessage("creator.base.property.open_direction"))
            .propertyValueSupplier(() ->
            {
                final @Nullable MovementDirection openDir0 = getMovementDirection();
                return openDir0 == null ? "NULL" : localizer.getMessage(openDir0.getLocalizationKey());
            })
            .updatable(true)
            .textSupplier(this::setOpenDirectionTextSupplier);

        factoryReviewResult = stepFactory
            .stepName(localizer, "REVIEW_RESULT")
            .stepExecutor(new StepExecutorBoolean(ignored -> true))
            .stepPreparation(this::prepareReviewResult)
            .textSupplier(this::reviewResultTextSupplier);

        factoryConfirmPrice = stepFactory
            .stepName(localizer, "CONFIRM_STRUCTURE_PRICE")
            .stepExecutor(new StepExecutorBoolean(this::confirmPrice))
            .skipCondition(this::skipConfirmPrice)
            .textSupplier(this::confirmPriceTextSupplier)
            .implicitNextStep(false);

        factoryCompleteProcess = stepFactory
            .stepName(localizer, "COMPLETE_CREATION_PROCESS")
            .stepExecutor(new StepExecutorVoid(this::completeCreationProcess))
            .textSupplier(text -> text.append(
                localizer.getMessage("creator.base.success"), TextType.SUCCESS, getStructureArg()))
            .waitForUserInput(false);
    }

    @Override
    protected synchronized void init()
    {
        super.init();

        if (name == null || !handleInput(name).join())
            runWithLock(this::prepareCurrentStep).join();
    }

    /**
     * Sets a property of the structure.
     *
     * @param property
     *     The property to set.
     * @param value
     *     The value to set the property to.
     * @param <T>
     *     The type of the property.
     * @throws IllegalArgumentException
     *     If the property is not valid for the structure type this property container was created for.
     */
    protected synchronized final <T> void setProperty(Property<T> property, T value)
    {
        propertyContainer.setPropertyValue(property, value);
    }

    /**
     * Gets a property of the structure.
     *
     * @param property
     *     The property to get.
     * @param <T>
     *     The type of the property.
     * @return The value of the property.
     */
    protected synchronized final <T> @Nullable T getProperty(Property<T> property)
    {
        return propertyContainer.getPropertyValue(property).value();
    }

    /**
     * Gets a required property of the structure.
     *
     * @param property
     *     The property to get.
     * @param <T>
     *     The type of the property.
     * @return The value of the property.
     *
     * @throws NullPointerException
     *     If the property is not set.
     */
    protected synchronized final <T> T getRequiredProperty(Property<T> property)
    {
        return Util.requireNonNull(getProperty(property), property.getFullKey());
    }

    /**
     * Shortcut method for creating a new highlighted argument of the structure type.
     * <p>
     * Can be used for Text object.
     *
     * @return The function that creates a new structure arg.
     */
    protected final Text.ArgumentCreator getStructureArg()
    {
        return arg -> arg.localizedHighlight(getStructureType());
    }

    /**
     * Updates a step with a given name and value.
     *
     * @param stepName
     *     The name of the step to update.
     * @param stepValue
     *     The value to update the step with.
     * @return A {@link CompletableFuture} that completes when the step has been updated.
     *
     * @throws IllegalStateException
     *     If the process is not in an updatable state.
     */
    public final synchronized CompletableFuture<Boolean> update(String stepName, @Nullable Object stepValue)
    {
        if (!processIsUpdatable)
            throw new IllegalStateException(
                "Trying to update step " + stepName + " with value " + stepValue +
                    " while the process is not in an updatable state!");
        processIsUpdatable = false;

        return runWithLock(
            () ->
            {
                insertStep(stepName);
                return prepareCurrentStep();
            })
            .withExceptionContext(
                "Handle updated step '%s' with value '%s' in Creator '%s'",
                stepName,
                stepValue,
                this
            );
    }

    /**
     * Handles an exception that occurred during the creation process.
     * <p>
     * This method will handle the situation as follows:
     * <ul>
     *     <li>Log the exception at SEVERE level.</li>
     *     <li>Send an error message to the player.</li>
     *     <li>Abort the creation process.</li>
     * </ul>
     *
     * @param ex
     *     The exception that occurred.
     * @param context
     *     The context in which the exception occurred. E.g. "prepare_set_open_status".
     */
    protected final void handleExceptional(Throwable ex, String context)
    {
        log.atSevere().withCause(ex).log("Failed to %s for Creator '%s'", context, this);
        getPlayer().sendError("creator.base.error.creation_cancelled");
        this.abort();
    }

    /**
     * Prepares the step that sets the open status.
     */
    protected void prepareSetOpenStatus()
    {
        commandFactory
            .getSetOpenStatusDelayed()
            .runDelayed(getPlayer(), this, this::handleInput, null)
            .orTimeout(10, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleExceptional(ex, "prepare_set_open_status"));
    }

    /**
     * Prepares the step that sets the open direction.
     */
    protected void prepareSetOpenDirection()
    {
        commandFactory
            .getSetOpenDirectionDelayed()
            .runDelayed(getPlayer(), this, this::handleInput, null)
            .orTimeout(10, TimeUnit.SECONDS)
            .handleExceptional(ex -> handleExceptional(ex, "prepare_set_open_direction"));
    }

    /**
     * Constructs the {@link Structure} for the current structure. This is the same for all structures.
     *
     * @param component
     *     The component to use for the structure. Leave null to use the default component.
     * @return The {@link Structure} for the current structure.
     */
    protected final synchronized Structure constructStructureData(@Nullable IStructureComponent component)
    {
        final var owner =
            new StructureOwner(structureID.getId(), PermissionLevel.CREATOR, getPlayer().getPlayerData());

        return structureBuilder
            .builder(structureType, component)
            .uid(structureID)
            .name(Util.requireNonNull(name, "Name"))
            .cuboid(Util.requireNonNull(cuboid, "cuboid"))
            .powerBlock(Util.requireNonNull(powerblock, "powerblock"))
            .world(Util.requireNonNull(world, "world"))
            .isLocked(isLocked)
            .openDir(Util.requireNonNull(movementDirection, "openDir"))
            .primeOwner(owner)
            .ownersOfStructure(null)
            .propertiesOfStructure(propertyContainer)
            .build();
    }

    protected synchronized void showPreview()
    {
        if (structureAnimationRequestBuilder == null)
        {
            log.atWarning().log("No StructureAnimationRequestBuilder available for Creator '%s'", this);
            return;
        }

        structureAnimationRequestBuilder
            .builder()
            .structure(constructStructure())
            .structureActionCause(StructureActionCause.PLUGIN)
            .structureActionType(StructureActionType.TOGGLE)
            .animationType(AnimationType.PREVIEW)
            .messageReceiver(getPlayer())
            .responsible(getPlayer())
            .build()
            .execute()
            .orTimeout(10, TimeUnit.SECONDS)
            .handleExceptional(ex -> log.atSevere().withCause(ex).log("Failed to show preview for Creator '%s'", this));
    }

    protected synchronized void prepareReviewResult()
    {
        try
        {
            showPreview();
        }
        catch (Exception e)
        {
            log.atSevere().withCause(e).log("Failed to create structure preview!");
            getPlayer().sendError("constants.error.generic");
        }
        this.processIsUpdatable = true;
    }

    /**
     * Completes the creation process. It'll construct and insert the structure and complete the {@link ToolUser}
     * process.
     *
     * @return True, so that it fits the functional interface being used for the steps.
     * <p>
     * If the insertion fails for whatever reason, it'll just be ignored, because at that point, there's no sense in
     * continuing the creation process anyway.
     */
    protected synchronized boolean completeCreationProcess()
    {
        removeTool();
        if (super.isActive())
            insertStructure(constructStructure());
        structureActivityManager.stopAnimators(this.structureID.getId());
        return true;
    }

    private synchronized void giveTool0()
    {
        if (!super.setPlayerHasTool(true))
            giveTool();
    }

    /**
     * Adds the AnimatedArchitecture tool from the player's inventory.
     *
     * @param nameKey
     *     The localization key of the name of the tool.
     * @param loreKey
     *     The localization key of the lore of the tool.
     */
    protected final void giveTool(String nameKey, String loreKey)
    {
        super.giveTool(
            nameKey,
            loreKey,
            getPlayer().newText().append(
                localizer.getMessage("creator.base.received_tool"),
                TextType.INFO,
                getStructureArg())
        );
    }

    /**
     * Method used to give the AnimatedArchitecture tool to the user.
     * <p>
     * Overriding methods may call {@link #giveTool(String, String, Text)}.
     */
    protected abstract void giveTool();

    /**
     * Completes the naming step for this {@link Creator}. This means that it'll set the name, go to the next step, and
     * give the user the creator tool.
     * <p>
     * Note that there are some requirements that the name must meet. See
     * {@link StringUtil#isValidStructureName(String)}.
     *
     * @param str
     *     The desired name of the structure.
     * @return True if the naming step was finished successfully.
     */
    protected synchronized boolean completeNamingStep(String str)
    {
        if (!StringUtil.isValidStructureName(str))
        {
            log.atFine().log("Invalid name '%s' for selected Creator: %s", str, this);
            getPlayer().sendError(
                "creator.base.error.invalid_name",
                arg -> arg.highlight(str),
                arg -> arg.localizedHighlight(getStructureType())
            );
            return false;
        }

        name = str;
        giveTool0();
        return true;
    }

    /**
     * Provides the first location of the selection and advances the procedure if successful.
     *
     * @param loc
     *     The first location of the cuboid.
     * @return True if setting the location was successful.
     */
    protected CompletableFuture<Boolean> provideFirstPos(ILocation loc)
    {
        return playerHasAccessToLocation(loc)
            .thenApply(isAllowed ->
            {
                if (!isAllowed)
                    return false;

                synchronized (this)
                {
                    world = loc.getWorld();
                    firstPos = loc.getPosition();
                }
                return true;
            })
            .orTimeout(10, TimeUnit.SECONDS)
            .withExceptionContext("Provide first position '%s' for Creator '%s'", loc, this);
    }

    /**
     * Creates a new {@link Cuboid} with the first position and the combined with position.
     * <p>
     * If the created {@link Cuboid} exceeds the size limit, the player will be informed and the result will be empty.
     *
     * @param combinedWith
     *     The position to combine with the first position.
     * @return The newly-created {@link Cuboid} if the volume is within the limits, otherwise an empty {@link Optional}.
     */
    protected final synchronized Optional<Cuboid> createCuboid(Vector3Di combinedWith)
    {
        final Cuboid newCuboid = new Cuboid(
            Util.requireNonNull(firstPos, "firstPos"),
            combinedWith
        );

        final OptionalInt sizeLimit = limitsManager.getLimit(getPlayer(), Limit.STRUCTURE_SIZE);
        if (sizeLimit.isPresent() && newCuboid.getVolume() > sizeLimit.getAsInt())
        {
            getPlayer().sendError(
                "creator.base.error.area_too_big",
                arg -> arg.localizedHighlight(getStructureType()),
                arg -> arg.highlight(newCuboid.getVolume()),
                arg -> arg.highlight(sizeLimit.getAsInt())
            );
            return Optional.empty();
        }
        return Optional.of(newCuboid);
    }

    /**
     * Provides the second location of the selection and advances the procedure if successful.
     *
     * @param loc
     *     The second location of the cuboid.
     * @return True if setting the location was successful.
     */
    protected CompletableFuture<Boolean> provideSecondPos(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return CompletableFuture.completedFuture(false);

        return playerHasAccessToLocation(loc)
            .<Optional<Cuboid>>thenApply(isAllowed ->
            {
                if (!isAllowed)
                    return Optional.empty();
                return createCuboid(loc.getPosition());
            })
            .thenCompose(cuboidOpt ->
                cuboidOpt
                    .map(newCuboid -> playerHasAccessToCuboid(newCuboid, Util.requireNonNull(getWorld(), "world")))
                    .orElse(CompletableFuture.completedFuture(Optional.empty())))
            .thenApply(newCuboid ->
            {
                if (newCuboid.isEmpty())
                    return false;

                synchronized (this)
                {
                    cuboid = newCuboid.get();
                }
                return true;
            })
            .orTimeout(10, TimeUnit.SECONDS)
            .withExceptionContext("Provide second position '%s' for Creator '%s'", loc, this);
    }

    /**
     * Attempts to buy the structure for the player and advances the procedure if successful.
     * <p>
     * Note that if the player does not end up buying the structure, either because of insufficient funds or because
     * they rejected the offer, the current step is NOT advanced!
     *
     * @param confirm
     *     Whether the player confirmed they want to buy this structure.
     * @return Always returns true, because either they can and do buy the structure, or they cannot or refuse to buy
     * the structure and the process is aborted.
     */
    // This method always returns the same value (S3516). However, in the case of this method, there is no reason to
    // return false as every input is valid and leads to a valid state.
    @SuppressWarnings("squid:S3516")
    protected synchronized boolean confirmPrice(boolean confirm)
    {
        if (!confirm)
        {
            getPlayer().sendError("creator.base.error.creation_cancelled");
            abort();
            return true;
        }
        if (!buyStructure())
        {
            getPlayer().sendError(
                "creator.base.error.insufficient_funds",
                arg -> arg.localizedHighlight(getStructureType()),
                arg -> arg.highlight(getPrice().orElse(0))
            );
            abort();
            return true;
        }

        goToNextStep();
        return true;
    }

    /**
     * Attempts to complete the step that provides the value of the {@link Property#OPEN_STATUS}.
     *
     * @param isOpen
     *     True if the current status of the structure is open.
     * @return True if the open status was set successfully.
     */
    protected synchronized boolean completeSetOpenStatusStep(boolean isOpen)
    {
        setProperty(Property.OPEN_STATUS, isOpen);
        return true;
    }

    /**
     * Attempts to complete the step that provides the {@link #movementDirection}.
     * <p>
     * If the open direction is not valid for this type, nothing changes.
     *
     * @param direction
     *     The {@link MovementDirection} that was selected by the player.
     * @return True if the {@link #movementDirection} was set successfully.
     */
    protected synchronized boolean completeSetOpenDirStep(MovementDirection direction)
    {
        if (!getValidOpenDirections().contains(direction))
        {
            getPlayer().sendError(
                "creator.base.error.invalid_option",
                arg -> arg.localizedHighlight(direction.getLocalizationKey())
            );
            prepareSetOpenDirection();
            return false;
        }
        movementDirection = direction;
        return true;
    }

    /**
     * Constructs the structure at the end of the creation process.
     *
     * @return The newly-created structure.
     */
    protected Structure constructStructure()
    {
        return constructStructureData(null);
    }

    /**
     * Verifies that the world of the selected location matches the world that this structure is being created in.
     *
     * @param targetWorld
     *     The world to check.
     * @return True if the world is the same world this structure is being created in.
     */
    protected synchronized boolean verifyWorldMatch(IWorld targetWorld)
    {
        if (Util.requireNonNull(world, "world").worldName().equals(targetWorld.worldName()))
            return true;

        getPlayer().sendError("creator.base.error.world_mismatch");

        log.atFine().log("World mismatch in ToolUser for player: %s", getPlayer());
        return false;
    }

    /**
     * Takes care of inserting the structure.
     *
     * @param structure
     *     The structure to send to the {@link DatabaseManager}.
     */
    protected void insertStructure(Structure structure)
    {
        databaseManager
            .addStructure(structure, getPlayer())
            .thenAccept(result ->
            {
                if (result.cancelled())
                {
                    getPlayer().sendError("creator.base.error.creation_cancelled");
                    return;
                }

                if (result.structure().isEmpty())
                {
                    getPlayer().sendError("constants.error.generic");
                    log.atSevere().log("Failed to insert structure after creation!");
                }
            })
            .orTimeout(30, TimeUnit.SECONDS)
            .handleExceptional(ex ->
            {
                getPlayer().sendError("constants.error.generic");
                log.atSevere().withCause(ex).log("Failed to insert structure after creation!");
            });
    }

    /**
     * Attempts to buy the structure for the current player.
     *
     * @return True if the player has bought the structure or if the economy is not enabled.
     */
    protected synchronized boolean buyStructure()
    {
        if (!economyManager.isEconomyEnabled())
            return true;

        return economyManager.buyStructure(
            getPlayer(),
            Util.requireNonNull(world, "world"),
            getStructureType(),
            Util.requireNonNull(cuboid, "cuboid").getVolume()
        );
    }

    /**
     * Gets the price of the structure based on its volume. If the structure is free because the price is &lt;= 0 or the
     * {@link IEconomyManager} is disabled, the price will be empty.
     *
     * @return The price of the structure if a positive price could be found.
     */
    protected synchronized OptionalDouble getPrice()
    {
        if (!economyManager.isEconomyEnabled())
            return OptionalDouble.empty();
        return economyManager.getPrice(getStructureType(), Util.requireNonNull(cuboid, "cuboid").getVolume());
    }

    /**
     * Checks if the step that asks the user to confirm that they want to buy the structure should be skipped.
     * <p>
     * It should be skipped if the structure is free for whatever reason. See {@link #getPrice()}.
     *
     * @return True if the step that asks the user to confirm that they want to buy the structure should be skipped.
     */
    protected boolean skipConfirmPrice()
    {
        return getPrice().isEmpty();
    }

    /**
     * Gets the list of valid open directions for this type. It returns a subset of
     * {@link StructureType#getValidMovementDirections()} based on the current physical aspects of the
     * {@link Structure}.
     *
     * @return The list of valid open directions for this type given its current physical dimensions.
     */
    public Set<MovementDirection> getValidOpenDirections()
    {
        return getStructureType().getValidMovementDirections();
    }

    /**
     * Checks if the power block is within the range limit.
     * <p>
     * If the power block is too far away, the player will be informed and the method will return false.
     * <p>
     * If the distance limit is not set, the method will return true.
     * <p>
     * If the power block is inside the structure, the player will be informed and the method will return false.
     *
     * @param pos
     *     The position of the power block.
     * @param cuboid
     *     The cuboid that defines the structure.
     * @return True if the power block is within the range limit.
     */
    protected final boolean isPowerBlockWithinRangeLimit(Vector3Di pos, Cuboid cuboid)
    {
        final int distance = cuboid.getDistanceToPoint(pos);
        if (distance == -1)
        {
            getPlayer().sendError(
                "creator.base.error.powerblock_inside_structure",
                arg -> arg.localizedHighlight(getStructureType())
            );
            return false;
        }

        final OptionalInt distanceLimit = limitsManager.getLimit(getPlayer(), Limit.POWERBLOCK_DISTANCE);
        if (distanceLimit.isEmpty() || distance <= distanceLimit.getAsInt())
            return true;

        getPlayer().sendError(
            "creator.base.error.powerblock_too_far",
            arg -> arg.localizedHighlight(getStructureType()),
            arg -> arg.highlight(distance),
            arg -> arg.highlight(distanceLimit.getAsInt())
        );
        return false;
    }

    /**
     * Attempts to complete the step in the {@link Procedure} that sets the second position of the {@link Structure}
     * that is being created.
     *
     * @param loc
     *     The selected location of the rotation point.
     * @return True if the location of the area was set successfully.
     */
    protected synchronized CompletableFuture<Boolean> completeSetPowerBlockStep(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return CompletableFuture.completedFuture(false);

        final Vector3Di pos = loc.getPosition();

        if (!isPowerBlockWithinRangeLimit(pos, Util.requireNonNull(cuboid, "cuboid")))
            return CompletableFuture.completedFuture(false);

        return playerHasAccessToLocation(loc)
            .thenApply(isAllowed ->
            {
                if (!isAllowed)
                    return false;
                setPowerblock(pos);
                return true;
            })
            .orTimeout(10, TimeUnit.SECONDS)
            .withExceptionContext("Provide power block position '%s' for Creator '%s'", loc, this);
    }

    /**
     * Attempts to complete the step in the {@link Procedure} that sets the location of the rotation point for the
     * {@link Structure} that is being created.
     *
     * @param loc
     *     The selected location of the rotation point.
     * @return True if the location of the rotation point was set successfully.
     */
    protected synchronized boolean completeSetRotationPointStep(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        if (!Util.requireNonNull(cuboid, "cuboid").isInRange(loc, 1))
        {
            log.atFinest().log("Rotation point not in range of cuboid for player: %s", getPlayer());
            getPlayer().sendError("creator.base.error.invalid_rotation_point");
            return false;
        }

        setProperty(Property.ROTATION_POINT, loc.getPosition());
        return true;
    }

    protected Text setOpenStatusTextSupplier(Text text)
    {
        return text.append(
            localizer.getMessage("creator.base.set_open_status"),
            TextType.INFO,

            arg -> arg.localizedHighlight(getStructureType()),

            arg -> arg.clickable(
                localizer.getMessage("constants.open_status.open"),
                "/animatedarchitecture SetOpenStatus " + localizer.getMessage("constants.open_status.open"),
                localizer.getMessage("creator.base.set_open_status.arg2.open.hint")
            ),

            arg -> arg.clickable(
                localizer.getMessage("constants.open_status.closed"),
                "/animatedarchitecture SetOpenStatus " + localizer.getMessage("constants.open_status.closed"),
                localizer.getMessage("creator.base.set_open_status.arg2.closed.hint"))
        );
    }

    protected Text setOpenDirectionTextSupplier(Text text)
    {
        text.append(
            localizer.getMessage("creator.base.set_open_direction") + "\n",
            TextType.INFO,
            arg -> arg.localizedHighlight(getStructureType())
        );

        getValidOpenDirections()
            .stream()
            .map(dir -> localizer.getMessage(dir.getLocalizationKey()))
            .sorted()
            .forEach(dir -> text.appendClickableText(
                dir + "\n", TextType.CLICKABLE,
                "/animatedarchitecture SetOpenDirection " + dir,
                localizer.getMessage("creator.base.set_open_direction.arg0.hint"))
            );

        return text;
    }

    private Text reviewResultTextSupplier(Text text)
    {
        text.append(localizer.getMessage("creator.base.review_result.header") + "\n", TextType.SUCCESS);
        text.append(
            localizer.getMessage("creator.base.property.type") + "\n",
            TextType.INFO,
            arg -> arg.localizedHighlight(getStructureType())
        );

        for (final Step step : getAllSteps())
            step.appendPropertyText(getPlayer().newText());

        text.append(
            localizer.getMessage("creator.base.review_result.footer"),
            TextType.INFO,
            arg -> arg.clickable(
                localizer.getMessage("creator.base.review_result.footer.arg0.message"),
                TextType.CLICKABLE_CONFIRM,
                "/animatedarchitecture confirm",
                localizer.getMessage("creator.base.review_result.footer.arg0.hint")),
            arg -> arg.clickable(
                localizer.getMessage("creator.base.review_result.footer.arg1.message"),
                TextType.CLICKABLE_REFUSE,
                "/animatedarchitecture cancel",
                localizer.getMessage("creator.base.review_result.footer.arg1.hint"))
        );
        return text;
    }

    private Text confirmPriceTextSupplier(Text text)
    {
        return text.append(
            localizer.getMessage("creator.base.confirm_structure_price"),
            TextType.INFO,

            arg -> arg.localizedInfo(getStructureType()),
            arg -> arg.highlight(getPrice().orElse(0)),

            arg -> arg.clickable(
                localizer.getMessage("creator.base.confirm_structure_price.arg2.message"),
                TextType.CLICKABLE_CONFIRM,
                "/animatedarchitecture confirm",
                localizer.getMessage("creator.base.confirm_structure_price.arg2.hint")),

            arg -> arg.clickable(
                localizer.getMessage("creator.base.confirm_structure_price.arg3.message"),
                TextType.CLICKABLE_REFUSE,
                "/animatedarchitecture cancel",
                localizer.getMessage("creator.base.confirm_structure_price.arg3.hint"))
        );
    }

    private String formatVector(@Nullable Vector3Di vector)
    {
        return vector == null ? "NULL" : String.format("%d, %d, %d", vector.x(), vector.y(), vector.z());
    }

    /**
     * Gets the name of the structure that is to be created.
     *
     * @return The name of the structure that is to be created.
     */
    protected final synchronized @Nullable String getName()
    {
        return name;
    }

    /**
     * Returns the cuboid of the structure that is to be created.
     *
     * @return The cuboid of the structure that is to be created or null if it has not been set yet.
     */
    protected final synchronized @Nullable Cuboid getCuboid()
    {
        return cuboid;
    }

    /**
     * Returns the movement direction of the structure that is to be created.
     *
     * @return The movement direction of the structure that is to be created or null if it has not been set yet.
     */
    protected final synchronized @Nullable MovementDirection getMovementDirection()
    {
        return movementDirection;
    }

    /**
     * Returns the location of the power block of the structure that is to be created.
     *
     * @return The power block of the structure that is to be created or null if it has not been set yet.
     */
    protected final synchronized @Nullable Vector3Di getPowerBlock()
    {
        return powerblock;
    }

    /**
     * Returns the first position of the structure that is to be created.
     * <p>
     * This is used together with a second position to construct {@link #cuboid}.
     *
     * @return The first position of the structure that is to be created or null if it has not been set yet.
     */
    @SuppressWarnings("unused") // It is used by the generated toString method.
    protected final synchronized @Nullable Vector3Di getFirstPos()
    {
        return this.firstPos;
    }

    /**
     * Returns the location of the power block of the structure that is to be created.
     *
     * @return The location of the power block of the structure that is to be created or null if it has not been set
     * yet.
     */
    @SuppressWarnings("unused") // It is used by the generated toString method.
    protected final synchronized @Nullable Vector3Di getPowerblock()
    {
        return this.powerblock;
    }

    /**
     * Returns the world in which the structure that is to be created is located.
     *
     * @return The world in which the structure that is to be created is located or null if it has not been set yet.
     */
    protected final synchronized @Nullable IWorld getWorld()
    {
        return this.world;
    }

    /**
     * Whether the structure that is being created is locked.
     *
     * @return True if the structure that is being created is locked, false otherwise.
     * <p>
     * This may not have been set yet, in which case it defaults to false.
     */
    protected final synchronized boolean isLocked()
    {
        return this.isLocked;
    }

    /**
     * Whether the process is in a state where is may be updated outside the normal execution order.
     *
     * @return True if the process is in a state where is may be updated outside the normal execution order, false
     * otherwise.
     */
    protected final synchronized boolean isProcessIsUpdatable()
    {
        return this.processIsUpdatable;
    }

    /**
     * Sets the name of the structure that is to be created.
     * <p>
     * Note that this method sets it directly without any processing or validation. Use
     * {@link #provideSecondPos(ILocation)} to set it properly.
     *
     * @param cuboid
     *     The cuboid of the structure that is to be created.
     */
    protected final synchronized void setCuboid(Cuboid cuboid)
    {
        this.cuboid = cuboid;
    }

    /**
     * Sets the power block of the structure that is to be created.
     * <p>
     * Note that this method sets it directly without any processing or validation. Use
     * {@link #completeSetPowerBlockStep(ILocation)} to set it properly.
     *
     * @param powerblock
     *     The power block of the structure that is to be created.
     */
    protected final synchronized void setPowerblock(Vector3Di powerblock)
    {
        this.powerblock = powerblock;
    }

    /**
     * Sets the movement direction of the structure that is to be created.
     * <p>
     * Note that this method sets it directly without any processing or validation. Use
     * {@link #completeSetOpenDirStep(MovementDirection)} to set it properly.
     *
     * @param movementDirection
     *     The movement direction of the structure that is to be created.
     */
    protected final synchronized void setMovementDirection(MovementDirection movementDirection)
    {
        this.movementDirection = movementDirection;
    }

    /**
     * Sets the world in which the structure that is to be created is located.
     * <p>
     * Note that this method sets it directly without any processing or validation. Use
     * {@link #provideFirstPos(ILocation)} to set it properly.
     *
     * @param world
     *     The world in which the structure that is to be created is located.
     */
    protected final synchronized void setWorld(IWorld world)
    {
        this.world = world;
    }

    /**
     * Sets the first position of the structure that is to be created.
     * <p>
     * Note that this method sets it directly without any processing or validation. Use
     * {@link #provideFirstPos(ILocation)} to set it properly.
     *
     * @param firstPos
     *     The first position of the structure that is to be created.
     */
    protected final synchronized void setFirstPos(Vector3Di firstPos)
    {
        this.firstPos = firstPos;
    }

    /**
     * Sets the locked status of the structure that is to be created.
     * <p>
     * Note that this method sets it directly without any processing or validation. Use
     * {@link #completeSetOpenStatusStep(boolean)} to set it properly.
     *
     * @param locked
     *     True if the structure that is to be created is locked, false otherwise.
     */
    protected final synchronized void setLocked(boolean locked)
    {
        isLocked = locked;
    }

    /**
     * Gets the unregistered UID of the structure.
     * <p>
     * This is a unique ID that is used to identify the structure before it is registered in the database.
     *
     * @return The unregistered UID of the structure.
     */
    public final long getUnregisteredUID()
    {
        return structureID.getId();
    }
}
