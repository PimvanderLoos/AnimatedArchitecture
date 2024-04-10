package nl.pim16aap2.animatedarchitecture.core.tooluser.creator;

import com.google.errorprone.annotations.concurrent.GuardedBy;
import lombok.ToString;
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
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBaseBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.text.TextArgument;
import nl.pim16aap2.animatedarchitecture.core.text.TextArgumentFactory;
import nl.pim16aap2.animatedarchitecture.core.text.TextType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Procedure;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorBoolean;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorLocation;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorOpenDirection;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorString;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorVoid;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.Limit;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.Util;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Represents a specialization of the {@link ToolUser} that is used for creating new {@link AbstractStructure}s.
 *
 * @author Pim
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@Flogger
@ThreadSafe
public abstract class Creator extends ToolUser
{
    private static final AtomicLong STRUCTURE_UID_PLACEHOLDER_COUNTER = new AtomicLong(-1000L);

    protected final LimitsManager limitsManager;

    protected final StructureBaseBuilder structureBaseBuilder;

    protected final DatabaseManager databaseManager;

    protected final IEconomyManager economyManager;

    protected final CommandFactory commandFactory;

    private final StructureAnimationRequestBuilder structureAnimationRequestBuilder;

    private final StructureActivityManager structureActivityManager;

    protected final long structureUidPlaceholder = STRUCTURE_UID_PLACEHOLDER_COUNTER.getAndDecrement();

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
     * The position of the rotation point selected by the user.
     */
    @ToString.Include
    @GuardedBy("this")
    private @Nullable Vector3Di rotationPoint;

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
     * Whether the structure is created in the open (true) or closed (false) position.
     */
    @ToString.Include
    @GuardedBy("this")
    private boolean isOpen = false;

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
    protected Step.Factory factoryProvideName;

    /**
     * Factory for the {@link Step} that provides the first position of the area of the structure.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory factoryProvideFirstPos;

    /**
     * Factory for the {@link Step} that provides the second position of the area of the structure, thus completing the
     * {@link Cuboid}.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory factoryProvideSecondPos;

    /**
     * Factory for the {@link Step} that provides the position of the structure's rotation point.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory factoryProvideRotationPointPos;

    /**
     * Factory for the {@link Step} that provides the position of the structure's power block.
     */
    protected Step.Factory factoryProvidePowerBlockPos;

    /**
     * Factory for the {@link Step} that provides the open status of the structure.
     */
    protected Step.Factory factoryProvideOpenStatus;

    /**
     * Factory for the {@link Step} that provides the open direction of the structure.
     */
    protected Step.Factory factoryProvideOpenDir;

    /**
     * Factory for the {@link Step} that allows the player to confirm or reject the price of the structure.
     */
    protected Step.Factory factoryConfirmPrice;

    /**
     * Factory for the {@link Step} that allows players to review the created structure
     */
    protected Step.Factory factoryReviewResult;

    /**
     * Factory for the {@link Step} that completes this process.
     * <p>
     * Don't forget to set the message before using it!
     */
    protected Step.Factory factoryCompleteProcess;

    protected Creator(ToolUser.Context context, IPlayer player, @Nullable String name)
    {
        super(context, player);
        this.structureAnimationRequestBuilder = context.getStructureAnimationRequestBuilder();
        this.structureActivityManager = context.getStructureActivityManager();
        this.limitsManager = context.getLimitsManager();
        this.structureBaseBuilder = context.getStructureBaseBuilder();
        this.databaseManager = context.getDatabaseManager();
        this.economyManager = context.getEconomyManager();
        this.commandFactory = context.getCommandFactory();

        player.sendMessage(textFactory.newText().append(
            localizer.getMessage("creator.base.init"), TextType.INFO,
            arg -> arg.clickable(
                "/AnimatedArchitecture cancel", TextType.CLICKABLE_REFUSE, "/AnimatedArchitecture cancel")));

        if (name != null)
            handleInput(name);
        else
            prepareCurrentStep().join();
    }

    @Override
    protected synchronized void init()
    {
        factoryProvideName = stepFactory
            .stepName("SET_NAME")
            .stepExecutor(new StepExecutorString(this::completeNamingStep))
            .propertyName(localizer.getMessage("creator.base.property.type"))
            .propertyValueSupplier(this::getName)
            .updatable(true)
            .textSupplier(text -> text.append(
                localizer.getMessage("creator.base.give_name"), TextType.SUCCESS, getStructureArg()));

        factoryProvideFirstPos = stepFactory
            .stepName("SET_FIRST_POS")
            .stepExecutor(new StepExecutorLocation(this::provideFirstPos));

        factoryProvideSecondPos = stepFactory
            .stepName("SET_SECOND_POS")
            .propertyName(localizer.getMessage("creator.base.property.cuboid"))
            .propertyValueSupplier(
                () ->
                {
                    final @Nullable Cuboid cuboid0 = getCuboid();
                    return cuboid0 == null ? "[]" :
                           String.format("[%s; %s]", formatVector(cuboid0.getMin()), formatVector(cuboid0.getMax()));
                })
            .stepExecutor(new StepExecutorLocation(this::provideSecondPos));

        factoryProvideRotationPointPos = stepFactory
            .stepName("SET_ROTATION_POINT")
            .propertyName(localizer.getMessage("creator.base.property.rotation_point"))
            .propertyValueSupplier(() -> formatVector(getRotationPoint()))
            .updatable(true)
            .stepExecutor(new StepExecutorLocation(this::completeSetRotationPointStep));

        factoryProvidePowerBlockPos = stepFactory
            .stepName("SET_POWER_BLOCK_POS")
            .messageKey("creator.base.set_power_block")
            .propertyName(localizer.getMessage("creator.base.property.power_block_position"))
            .propertyValueSupplier(() -> formatVector(getPowerBlock()))
            .updatable(true)
            .stepExecutor(new StepExecutorLocation(this::completeSetPowerBlockStep));

        factoryProvideOpenStatus = stepFactory
            .stepName("SET_OPEN_STATUS")
            .stepExecutor(new StepExecutorBoolean(this::completeSetOpenStatusStep))
            .stepPreparation(this::prepareSetOpenStatus)
            .propertyName(localizer.getMessage("creator.base.property.open_status"))
            .propertyValueSupplier(
                () -> getIsOpen() ?
                      localizer.getMessage("constants.open_status.open") :
                      localizer.getMessage("constants.open_status.closed"))
            .updatable(true)
            .textSupplier(this::setOpenStatusTextSupplier);

        factoryProvideOpenDir = stepFactory
            .stepName("SET_OPEN_DIRECTION")
            .stepExecutor(new StepExecutorOpenDirection(this::completeSetOpenDirStep))
            .stepPreparation(this::prepareSetOpenDirection)
            .propertyName(localizer.getMessage("creator.base.property.open_direction"))
            .propertyValueSupplier(
                () ->
                {
                    final @Nullable MovementDirection openDir0 = getMovementDirection();
                    return openDir0 == null ? "NULL" : localizer.getMessage(openDir0.getLocalizationKey());
                })
            .updatable(true)
            .textSupplier(this::setOpenDirectionTextSupplier);

        factoryReviewResult = stepFactory
            .stepName("REVIEW_RESULT")
            .stepExecutor(new StepExecutorBoolean(ignored -> true))
            .stepPreparation(this::prepareReviewResult)
            .textSupplier(this::reviewResultTextSupplier);

        factoryConfirmPrice = stepFactory
            .stepName("CONFIRM_STRUCTURE_PRICE")
            .stepExecutor(new StepExecutorBoolean(this::confirmPrice))
            .skipCondition(this::skipConfirmPrice)
            .textSupplier(this::confirmPriceTextSupplier)
            .implicitNextStep(false);

        factoryCompleteProcess = stepFactory
            .stepName("COMPLETE_CREATION_PROCESS")
            .stepExecutor(new StepExecutorVoid(this::completeCreationProcess))
            .textSupplier(text -> text.append(
                localizer.getMessage("creator.base.success"), TextType.SUCCESS, getStructureArg()))
            .waitForUserInput(false);
    }

    /**
     * Shortcut method for creating a new highlighted argument of the structure type.
     * <p>
     * Can be used for Text object.
     *
     * @return The function that creates a new structure arg.
     */
    protected final synchronized Function<TextArgumentFactory, TextArgument> getStructureArg()
    {
        return arg -> arg.highlight(localizer.getStructureType(getStructureType()));
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
        getProcedure().insertStep(stepName);
        return prepareCurrentStep().thenCompose(ignored -> handleInput(stepValue));
    }

    /**
     * Prepares the step that sets the open status.
     */
    protected synchronized void prepareSetOpenStatus()
    {
        commandFactory.getSetOpenStatusDelayed().runDelayed(getPlayer(), this, status ->
                          CompletableFuture.completedFuture(handleInput(status)), null)
                      .exceptionally(Util::exceptionally);
    }

    /**
     * Prepares the step that sets the open direction.
     */
    protected synchronized void prepareSetOpenDirection()
    {
        commandFactory
            .getSetOpenDirectionDelayed()
            .runDelayed(getPlayer(), this, this::handleInput, null)
            .exceptionally(Util::exceptionally);
    }

    /**
     * Constructs the {@link AbstractStructure.BaseHolder} for the current structure. This is the same for all
     * structures.
     *
     * @return The {@link AbstractStructure.BaseHolder} for the current structure.
     */
    protected final synchronized AbstractStructure.BaseHolder constructStructureData()
    {
        final var owner =
            new StructureOwner(structureUidPlaceholder, PermissionLevel.CREATOR, getPlayer().getPlayerData());

        return structureBaseBuilder
            .builder()
            .uid(structureUidPlaceholder)
            .name(Util.requireNonNull(name, "Name"))
            .cuboid(Util.requireNonNull(cuboid, "cuboid"))
            .rotationPoint(Util.requireNonNull(rotationPoint, "rotationPoint"))
            .powerBlock(Util.requireNonNull(powerblock, "powerblock"))
            .world(Util.requireNonNull(world, "world"))
            .isOpen(isOpen)
            .isLocked(isLocked)
            .openDir(Util.requireNonNull(movementDirection, "openDir"))
            .primeOwner(owner)
            .build();
    }

    protected synchronized void showPreview()
    {
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
            .exceptionally(Util::exceptionally);
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
            getPlayer().sendMessage(
                textFactory.newText().append(localizer.getMessage("constants.error.generic"), TextType.ERROR));
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
        structureActivityManager.stopAnimators(this.structureUidPlaceholder);
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
    protected final synchronized void giveTool(String nameKey, String loreKey)
    {
        super.giveTool(nameKey, loreKey, textFactory.newText().append(
            localizer.getMessage("creator.base.received_tool"), TextType.INFO, getStructureArg()));
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
     * Note that there are some requirements that the name must meet. See {@link Util#isValidStructureName(String)}.
     *
     * @param str
     *     The desired name of the structure.
     * @return True if the naming step was finished successfully.
     */
    protected synchronized boolean completeNamingStep(String str)
    {
        if (!Util.isValidStructureName(str))
        {
            log.atFine().log("Invalid name '%s' for selected Creator: %s", str, this);
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.base.error.invalid_name"), TextType.ERROR,
                arg -> arg.highlight(str),
                arg -> arg.highlight(localizer.getStructureType(getStructureType()))));
            return false;
        }

        name = str;
        giveTool0();
        return true;
    }

    /**
     * Sets the first location of the selection and advances the procedure if successful.
     *
     * @param loc
     *     The first location of the cuboid.
     * @return True if setting the location was successful.
     */
    protected synchronized boolean provideFirstPos(ILocation loc)
    {
        if (!playerHasAccessToLocation(loc))
            return false;

        world = loc.getWorld();
        firstPos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return true;
    }

    /**
     * Provides the second location of the selection and advances the procedure if successful.
     *
     * @param loc
     *     The second location of the cuboid.
     * @return True if setting the location was successful.
     */
    protected synchronized boolean provideSecondPos(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        final Cuboid newCuboid = new Cuboid(
            Util.requireNonNull(firstPos, "firstPos"),
            new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

        final OptionalInt sizeLimit = limitsManager.getLimit(getPlayer(), Limit.STRUCTURE_SIZE);
        if (sizeLimit.isPresent() && newCuboid.getVolume() > sizeLimit.getAsInt())
        {
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.base.error.area_too_big"), TextType.ERROR,
                arg -> arg.highlight(localizer.getStructureType(getStructureType())),
                arg -> arg.highlight(newCuboid.getVolume()),
                arg -> arg.highlight(sizeLimit.getAsInt())));
            return false;
        }

        cuboid = newCuboid;

        return playerHasAccessToCuboid(cuboid, Util.requireNonNull(world, "world"));
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
            getPlayer().sendMessage(textFactory, TextType.INFO,
                                    localizer.getMessage("creator.base.error.creation_cancelled"));
            abort();
            return true;
        }
        if (!buyStructure())
        {
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.base.error.insufficient_funds"), TextType.ERROR,
                arg -> arg.highlight(localizer.getStructureType(getStructureType())),
                arg -> arg.highlight(getPrice().orElse(0))));
            abort();
            return true;
        }

        getProcedure().goToNextStep();
        return true;
    }

    /**
     * Attempts to complete the step that provides the {@link #isOpen}.
     *
     * @param isOpen
     *     True if the current status of the structure is open.
     * @return True if the {@link #isOpen} was set successfully.
     */
    protected synchronized boolean completeSetOpenStatusStep(boolean isOpen)
    {
        this.isOpen = isOpen;
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
            getPlayer().sendMessage(textFactory.newText().append(
                localizer.getMessage("creator.base.error.invalid_option"), TextType.ERROR,
                arg -> arg.highlight(localizer.getMessage(direction.getLocalizationKey()))));
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
    protected abstract AbstractStructure constructStructure();

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
        log.atFine().log("World mismatch in ToolUser for player: %s", getPlayer());
        return false;
    }

    /**
     * Takes care of inserting the structure.
     *
     * @param structure
     *     The structure to send to the {@link DatabaseManager}.
     */
    protected synchronized void insertStructure(AbstractStructure structure)
    {
        databaseManager.addStructure(structure, getPlayer()).whenComplete(
            (result, throwable) ->
            {
                if (result.cancelled())
                {
                    getPlayer().sendError(
                        textFactory, localizer.getMessage("creator.base.error.creation_cancelled"));
                    return;
                }

                if (result.structure().isEmpty())
                {
                    getPlayer().sendError(
                        textFactory, localizer.getMessage("constants.error.generic"));
                    log.atSevere().log("Failed to insert structure after creation!");
                }
            }).exceptionally(Util::exceptionally);
    }

    /**
     * Obtains the type of structure this creator will create.
     *
     * @return The type of structure that will be created.
     */
    protected abstract StructureType getStructureType();

    /**
     * Attempts to buy the structure for the current player.
     *
     * @return True if the player has bought the structure or if the economy is not enabled.
     */
    protected synchronized boolean buyStructure()
    {
        if (!economyManager.isEconomyEnabled())
            return true;

        return economyManager.buyStructure(getPlayer(), Util.requireNonNull(world, "world"), getStructureType(),
                                           Util.requireNonNull(cuboid, "cuboid").getVolume());
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
    protected synchronized boolean skipConfirmPrice()
    {
        return getPrice().isEmpty();
    }

    /**
     * Gets the list of valid open directions for this type. It returns a subset of
     * {@link StructureType#getValidOpenDirections()} based on the current physical aspects of the
     * {@link AbstractStructure}.
     *
     * @return The list of valid open directions for this type given its current physical dimensions.
     */
    public synchronized Set<MovementDirection> getValidOpenDirections()
    {
        return getStructureType().getValidOpenDirections();
    }

    /**
     * Attempts to complete the step in the {@link Procedure} that sets the second position of the
     * {@link AbstractStructure} that is being created.
     *
     * @param loc
     *     The selected location of the rotation point.
     * @return True if the location of the area was set successfully.
     */
    protected synchronized boolean completeSetPowerBlockStep(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        final Vector3Di pos = loc.getPosition();
        if (Util.requireNonNull(cuboid, "cuboid").isPosInsideCuboid(pos))
        {
            getPlayer().sendMessage(textFactory.newText().append(
                "creator.base.error.powerblock_inside_structure", TextType.ERROR,
                arg -> arg.highlight(localizer.getStructureType(getStructureType()))));
            return false;
        }
        final OptionalInt distanceLimit = limitsManager.getLimit(getPlayer(), Limit.POWERBLOCK_DISTANCE);
        final double distance;
        if (distanceLimit.isPresent() &&
            (distance = cuboid.getCenter().getDistance(pos)) > distanceLimit.getAsInt())
        {
            getPlayer().sendMessage(textFactory.newText().append(
                "creator.base.error.powerblock_too_far", TextType.ERROR,
                arg -> arg.highlight(localizer.getStructureType(getStructureType())),
                arg -> arg.highlight(distance),
                arg -> arg.highlight(distanceLimit.getAsInt())));
            return false;
        }

        powerblock = pos;
        return true;
    }

    /**
     * Attempts to complete the step in the {@link Procedure} that sets the location of the rotation point for the
     * {@link AbstractStructure} that is being created.
     *
     * @param loc
     *     The selected location of the rotation point.
     * @return True if the location of the rotation point was set successfully.
     */
    protected synchronized boolean completeSetRotationPointStep(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        if (!Util.requireNonNull(cuboid, "cuboid").isInRange(loc, 1))
        {
            getPlayer().sendError(
                textFactory, localizer.getMessage("creator.base.error.invalid_rotation_point"));
            return false;
        }

        rotationPoint = loc.getPosition();
        return true;
    }

    protected synchronized Text setOpenStatusTextSupplier(Text text)
    {
        return text.append(
            localizer.getMessage("creator.base.set_open_status"), TextType.INFO,

            arg -> arg.highlight(localizer.getStructureType(getStructureType())),

            arg -> arg.clickable(
                localizer.getMessage("constants.open_status.open"),
                "/animatedarchitecture SetOpenStatus " + localizer.getMessage("constants.open_status.open"),
                localizer.getMessage("creator.base.set_open_status.arg2.open.hint")),

            arg -> arg.clickable(
                localizer.getMessage("constants.open_status.closed"),
                "/animatedarchitecture SetOpenStatus " + localizer.getMessage("constants.open_status.closed"),
                localizer.getMessage("creator.base.set_open_status.arg2.closed.hint")));
    }

    protected synchronized Text setOpenDirectionTextSupplier(Text text)
    {
        text.append(localizer.getMessage("creator.base.set_open_direction") + "\n", TextType.INFO,
                    arg -> arg.highlight(localizer.getStructureType(getStructureType())));

        getValidOpenDirections().stream().map(dir -> localizer.getMessage(dir.getLocalizationKey())).sorted().forEach(
            dir -> text.appendClickableText(
                dir + "\n", TextType.CLICKABLE,
                "/animatedarchitecture SetOpenDirection " + dir,
                localizer.getMessage("creator.base.set_open_direction.arg0.hint")));

        return text;
    }

    private synchronized Text reviewResultTextSupplier(Text text)
    {
        text.append(localizer.getMessage("creator.base.review_result.header") + "\n", TextType.SUCCESS);
        text.append(
            localizer.getMessage("creator.base.property.type") + "\n", TextType.INFO,
            arg -> arg.highlight(localizer.getStructureType(getStructureType())));

        for (final Step step : getProcedure().getAllSteps())
            step.getPropertyText(textFactory).ifPresent(property -> text.append(property).append('\n'));

        text.append(
            localizer.getMessage("creator.base.review_result.footer"), TextType.INFO,
            arg -> arg.clickable(
                localizer.getMessage("creator.base.review_result.footer.arg0.message"),
                TextType.CLICKABLE_CONFIRM,
                "/animatedarchitecture confirm",
                localizer.getMessage("creator.base.review_result.footer.arg0.hint")),
            arg -> arg.clickable(
                localizer.getMessage("creator.base.review_result.footer.arg1.message"),
                TextType.CLICKABLE_REFUSE,
                "/animatedarchitecture cancel",
                localizer.getMessage("creator.base.review_result.footer.arg1.hint")));
        return text;
    }

    private synchronized Text confirmPriceTextSupplier(Text text)
    {
        return text.append(
            localizer.getMessage("creator.base.confirm_structure_price"), TextType.INFO,

            arg -> arg.info(localizer.getStructureType(getStructureType())),
            arg -> arg.highlight(getPrice().orElse(0)),

            arg -> arg.clickable(
                localizer.getMessage("creator.base.confirm_structure_price.arg2.message"), TextType.CLICKABLE_CONFIRM,
                "/animatedarchitecture confirm",
                localizer.getMessage("creator.base.confirm_structure_price.arg2.hint")),

            arg -> arg.clickable(
                localizer.getMessage("creator.base.confirm_structure_price.arg3.message"), TextType.CLICKABLE_REFUSE,
                "/animatedarchitecture cancel",
                localizer.getMessage("creator.base.confirm_structure_price.arg3.hint"))
        );
    }

    private synchronized String formatVector(@Nullable Vector3Di vector)
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
     * Returns the open status of the structure that is to be created.
     * <p>
     * This may not have been set yet, in which case it defaults to false.
     *
     * @return The open status of the structure that is to be created.
     */
    protected final synchronized boolean getIsOpen()
    {
        return isOpen;
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
     * Returns the rotation point of the structure that is to be created.
     *
     * @return The rotation point of the structure that is to be created or null if it has not been set yet.
     */
    protected final synchronized @Nullable Vector3Di getRotationPoint()
    {
        return rotationPoint;
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
     * Whether the structure that is being created is open.
     *
     * @return True if the structure that is being created is open, false otherwise.
     * <p>
     * This may not have been set yet, in which case it defaults to false.
     */
    protected final synchronized boolean isOpen()
    {
        return this.isOpen;
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
    @SuppressWarnings("unused") // It is used by the generated toString method.
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
    @SuppressWarnings("unused")
    protected final synchronized void setCuboid(Cuboid cuboid)
    {
        this.cuboid = cuboid;
    }

    /**
     * Sets the rotation point of the structure that is to be created.
     * <p>
     * Note that this method sets it directly without any processing or validation. Use
     * {@link #completeSetRotationPointStep(ILocation)} to set it properly.
     *
     * @param rotationPoint
     *     The rotation point of the structure that is to be created.
     */
    @SuppressWarnings("unused")
    protected final synchronized void setRotationPoint(Vector3Di rotationPoint)
    {
        this.rotationPoint = rotationPoint;
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
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
     * Sets the name of the structure that is to be created.
     * <p>
     * Note that this method sets it directly without any processing or validation. Use
     * {@link #completeNamingStep(String)} to set it properly.
     *
     * @param open
     *     True if the structure that is to be created is open, false otherwise.
     */
    @SuppressWarnings("unused")
    protected final synchronized void setOpen(boolean open)
    {
        isOpen = open;
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
    @SuppressWarnings("unused")
    protected final synchronized void setLocked(boolean locked)
    {
        isLocked = locked;
    }
}
