package nl.pim16aap2.animatedarchitecture.core.tooluser.creator;

import lombok.ToString;
import lombok.extern.flogger.Flogger;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.LimitsManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.PermissionLevel;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBaseBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureOwner;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a specialization of the {@link ToolUser} that is used for creating new {@link AbstractStructure}s.
 *
 * @author Pim
 */
@ToString(callSuper = true)
@Flogger
public abstract class Creator extends ToolUser
{
    protected final LimitsManager limitsManager;

    protected final StructureBaseBuilder structureBaseBuilder;

    protected final DatabaseManager databaseManager;

    protected final IEconomyManager economyManager;

    protected final CommandFactory commandFactory;

    /**
     * The name of the structure that is to be created.
     */
    protected @Nullable String name;

    /**
     * The cuboid that defines the location and dimensions of the structure.
     * <p>
     * This region is defined by {@link #firstPos} and the second position selected by the user.
     */
    protected @Nullable Cuboid cuboid;

    /**
     * The first point that was selected in the process.
     * <p>
     * Once a second point has been selected, these two are used to construct the {@link #cuboid}.
     */
    protected @Nullable Vector3Di firstPos;

    /**
     * The position of the rotation point selected by the user.
     */
    protected @Nullable Vector3Di rotationPoint;

    /**
     * The powerblock selected by the user.
     */
    protected @Nullable Vector3Di powerblock;

    /**
     * The opening direction selected by the user.
     */
    protected @Nullable MovementDirection openDir;

    /**
     * The {@link IWorld} this structure is created in.
     */
    protected @Nullable IWorld world;

    /**
     * Whether the structure is created in the open (true) or closed (false) position.
     */
    protected boolean isOpen = false;

    /**
     * Whether the structure is created in the locked (true) or unlocked (false) state.
     */
    protected boolean isLocked = false;

    /**
     * IFactory for the {@link Step} that sets the name.
     */
    @ToString.Exclude
    protected Step.Factory factorySetName;

    /**
     * IFactory for the {@link Step} that sets the first position of the area of the structure.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factorySetFirstPos;

    /**
     * IFactory for the {@link Step} that sets the second position of the area of the structure, thus completing the
     * {@link Cuboid}.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factorySetSecondPos;

    /**
     * IFactory for the {@link Step} that sets the position of the structure's rotation point.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factorySetRotationPointPos;

    /**
     * IFactory for the {@link Step} that sets the position of the structure's power block.
     */
    @ToString.Exclude
    protected Step.Factory factorySetPowerBlockPos;

    /**
     * IFactory for the {@link Step} that sets the open status of the structure.
     */
    @ToString.Exclude
    protected Step.Factory factorySetOpenStatus;

    /**
     * IFactory for the {@link Step} that sets the open direction of the structure.
     */
    @ToString.Exclude
    protected Step.Factory factorySetOpenDir;

    /**
     * IFactory for the {@link Step} that allows the player to confirm or reject the price of the structure.
     */
    @ToString.Exclude
    protected Step.Factory factoryConfirmPrice;

    /**
     * IFactory for the {@link Step} that completes this process.
     * <p>
     * Don't forget to set the message before using it!
     */
    @ToString.Exclude
    protected Step.Factory factoryCompleteProcess;

    private static final MyDecimalFormat DECIMAL_FORMAT = new MyDecimalFormat();

    protected Creator(Context context, IPlayer player, @Nullable String name)
    {
        super(context, player);
        limitsManager = context.getLimitsManager();
        structureBaseBuilder = context.getStructureBaseBuilder();
        databaseManager = context.getDatabaseManager();
        economyManager = context.getEconomyManager();
        commandFactory = context.getCommandFactory();

        player.sendMessage(textFactory, TextType.INFO, localizer.getMessage("creator.base.init"));

        if (name != null)
            handleInput(name);
        else
            prepareCurrentStep();
    }

    @Override
    protected void init()
    {
        factorySetName = stepFactory
            .stepName("SET_NAME")
            .stepExecutor(new StepExecutorString(this::completeNamingStep))
            .textSupplier(
                text -> text.append(
                    localizer.getMessage("creator.base.give_name"), TextType.INFO,
                    text.newTextArgument(localizer.getStructureType(getStructureType()), TextType.HIGHLIGHT)));

        factorySetFirstPos = stepFactory
            .stepName("SET_FIRST_POS")
            .stepExecutor(new StepExecutorLocation(this::setFirstPos));

        factorySetSecondPos = stepFactory
            .stepName("SET_SECOND_POS")
            .stepExecutor(new StepExecutorLocation(this::setSecondPos));

        factorySetRotationPointPos = stepFactory
            .stepName("SET_ROTATION_POINT")
            .stepExecutor(new StepExecutorLocation(this::completeSetRotationPointStep));

        factorySetPowerBlockPos = stepFactory
            .stepName("SET_POWER_BLOCK_POS")
            .messageKey("creator.base.set_power_block")
            .stepExecutor(new StepExecutorLocation(this::completeSetPowerBlockStep));

        factorySetOpenStatus = stepFactory
            .stepName("SET_OPEN_STATUS")
            .stepExecutor(new StepExecutorBoolean(this::completeSetOpenStatusStep))
            .stepPreparation(this::prepareSetOpenStatus)
            .textSupplier(this::setOpenStatusTextSupplier);

        factorySetOpenDir = stepFactory
            .stepName("SET_OPEN_DIRECTION")
            .stepExecutor(new StepExecutorOpenDirection(this::completeSetOpenDirStep))
            .stepPreparation(this::prepareSetOpenDirection)
            .messageKey("creator.base.set_open_direction")
            .textSupplier(this::setOpenDirectionTextSupplier);

        factoryConfirmPrice = stepFactory
            .stepName("CONFIRM_STRUCTURE_PRICE")
            .stepExecutor(new StepExecutorBoolean(this::confirmPrice))
            .skipCondition(this::skipConfirmPrice)
            .textSupplier(this::confirmPriceTextSupplier)
            .implicitNextStep(false);

        factoryCompleteProcess = stepFactory
            .stepName("COMPLETE_CREATION_PROCESS")
            .stepExecutor(new StepExecutorVoid(this::completeCreationProcess))
            .waitForUserInput(false);
    }

    /**
     * Prepares the step that sets the open status.
     */
    protected void prepareSetOpenStatus()
    {
        commandFactory.getSetOpenStatusDelayed().runDelayed(getPlayer(), this, status ->
            CompletableFuture.completedFuture(handleInput(status)), null);
    }

    /**
     * Prepares the step that sets the open direction.
     */
    protected void prepareSetOpenDirection()
    {
        commandFactory.getSetOpenDirectionDelayed().runDelayed(getPlayer(), this, direction ->
            CompletableFuture.completedFuture(handleInput(direction)), null);
    }

    /**
     * Constructs the {@link AbstractStructure.BaseHolder} for the current structure. This is the same for all
     * structures.
     *
     * @return The {@link AbstractStructure.BaseHolder} for the current structure.
     */
    protected final AbstractStructure.BaseHolder constructStructureData()
    {
        final long structureUID = -1;
        final var owner = new StructureOwner(structureUID, PermissionLevel.CREATOR, getPlayer().getPlayerData());

        return structureBaseBuilder
            .builder()
            .uid(structureUID)
            .name(Util.requireNonNull(name, "Name"))
            .cuboid(Util.requireNonNull(cuboid, "cuboid"))
            .rotationPoint(Util.requireNonNull(rotationPoint, "rotationPoint"))
            .powerBlock(Util.requireNonNull(powerblock, "powerblock"))
            .world(Util.requireNonNull(world, "world"))
            .isOpen(isOpen)
            .isLocked(isLocked)
            .openDir(Util.requireNonNull(openDir, "openDir"))
            .primeOwner(owner)
            .build();
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
    protected boolean completeCreationProcess()
    {
        if (active)
            insertStructure(constructStructure());
        return true;
    }

    /**
     * Method used to give the AnimatedArchitecture tool to the user.
     * <p>
     * Overriding methods may call {@link #giveTool(String, String, String)}.
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
    protected boolean completeNamingStep(String str)
    {
        if (!Util.isValidStructureName(str))
        {
            log.atFine().log("Invalid name '%s' for selected Creator: %s", str, this);
            getPlayer().sendMessage(textFactory, TextType.ERROR,
                                    localizer.getMessage("creator.base.error.invalid_name",
                                                         str, localizer.getStructureType(getStructureType())));
            return false;
        }

        name = str;
        giveTool();
        return true;
    }

    /**
     * Sets the first location of the selection and advances the procedure if successful.
     *
     * @param loc
     *     The first location of the cuboid.
     * @return True if setting the location was successful.
     */
    protected boolean setFirstPos(ILocation loc)
    {
        if (!playerHasAccessToLocation(loc))
            return false;

        world = loc.getWorld();
        firstPos = new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return true;
    }

    /**
     * Sets the second location of the selection and advances the procedure if successful.
     *
     * @param loc
     *     The second location of the cuboid.
     * @return True if setting the location was successful.
     */
    protected boolean setSecondPos(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        final Cuboid newCuboid = new Cuboid(Util.requireNonNull(firstPos, "firstPos"),
                                            new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

        final OptionalInt sizeLimit = limitsManager.getLimit(getPlayer(), Limit.STRUCTURE_SIZE);
        if (sizeLimit.isPresent() && newCuboid.getVolume() > sizeLimit.getAsInt())
        {
            getPlayer().sendMessage(
                textFactory, TextType.ERROR,
                localizer.getMessage("creator.base.error.area_too_big",
                                     localizer.getStructureType(getStructureType()),
                                     Integer.toString(newCuboid.getVolume()),
                                     Integer.toString(sizeLimit.getAsInt())));
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
    protected boolean confirmPrice(boolean confirm)
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
            getPlayer().sendMessage(textFactory, TextType.ERROR,
                                    localizer.getMessage("creator.base.error.insufficient_funds",
                                                         localizer.getStructureType(getStructureType()),
                                                         DECIMAL_FORMAT.format(getPrice().orElse(0))));
            abort();
            return true;
        }

        getProcedure().goToNextStep();
        return true;
    }

    /**
     * Attempts to complete the step that sets the {@link #isOpen}.
     *
     * @param isOpen
     *     True if the current status of the structure is open.
     * @return True if the {@link #isOpen} was set successfully.
     */
    protected boolean completeSetOpenStatusStep(boolean isOpen)
    {
        this.isOpen = isOpen;
        return true;
    }

    /**
     * Attempts to complete the step that sets the {@link #openDir}.
     * <p>
     * If the open direction is not valid for this type, nothing changes.
     *
     * @param direction
     *     The {@link MovementDirection} that was selected by the player.
     * @return True if the {@link #openDir} was set successfully.
     */
    protected boolean completeSetOpenDirStep(MovementDirection direction)
    {
        if (!getValidOpenDirections().contains(direction))
        {
            getPlayer().sendMessage(textFactory, TextType.ERROR,
                                    localizer.getMessage("creator.base.error.invalid_option", direction.name()));
            prepareSetOpenDirection();
            return false;
        }
        openDir = direction;
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
    protected boolean verifyWorldMatch(IWorld targetWorld)
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
    protected void insertStructure(AbstractStructure structure)
    {
        databaseManager.addStructure(structure, getPlayer()).whenComplete(
            (result, throwable) ->
            {
                if (result.cancelled())
                {
                    getPlayer().sendMessage(textFactory, TextType.ERROR,
                                            localizer.getMessage("creator.base.error.creation_cancelled"));
                    return;
                }

                if (result.structure().isEmpty())
                {
                    getPlayer().sendMessage(textFactory, TextType.ERROR,
                                            localizer.getMessage("constants.error.generic"));
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
    protected boolean buyStructure()
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
    protected OptionalDouble getPrice()
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
     * {@link StructureType#getValidOpenDirections()} based on the current physical aspects of the
     * {@link AbstractStructure}.
     *
     * @return The list of valid open directions for this type given its current physical dimensions.
     */
    public Set<MovementDirection> getValidOpenDirections()
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
    protected boolean completeSetPowerBlockStep(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        final Vector3Di pos = loc.getPosition();
        if (Util.requireNonNull(cuboid, "cuboid").isPosInsideCuboid(pos))
        {
            getPlayer().sendError(textFactory,
                                  localizer.getMessage("creator.base.error.powerblock_inside_structure",
                                                       localizer.getStructureType(getStructureType())));
            return false;
        }
        final OptionalInt distanceLimit = limitsManager.getLimit(getPlayer(), Limit.POWERBLOCK_DISTANCE);
        final double distance;
        if (distanceLimit.isPresent() &&
            (distance = cuboid.getCenter().getDistance(pos)) > distanceLimit.getAsInt())
        {
            getPlayer().sendError(textFactory,
                                  localizer.getMessage("creator.base.error.powerblock_too_far",
                                                       localizer.getStructureType(getStructureType()),
                                                       DECIMAL_FORMAT.format(distance),
                                                       Integer.toString(distanceLimit.getAsInt())));
            return false;
        }

        powerblock = pos;

        removeTool();
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
    protected boolean completeSetRotationPointStep(ILocation loc)
    {
        if (!verifyWorldMatch(loc.getWorld()))
            return false;

        if (!playerHasAccessToLocation(loc))
            return false;

        if (!Util.requireNonNull(cuboid, "cuboid").isInRange(loc, 1))
        {
            getPlayer().sendMessage(textFactory, TextType.ERROR,
                                    localizer.getMessage("creator.base.error.invalid_rotation_point"));
            return false;
        }

        rotationPoint = loc.getPosition();
        return true;
    }

    protected Text setOpenStatusTextSupplier(Text text)
    {
        return text.append(
            localizer.getMessage("creator.base.set_open_status"), TextType.INFO,

            text.newTextArgument(localizer.getStructureType(getStructureType()), TextType.HIGHLIGHT),

            text.newClickableTextArgument(
                localizer.getMessage("constants.open_status.open"), TextType.CLICKABLE,
                "/animatedarchitecture SetOpenStatus " + localizer.getMessage("constants.open_status.open"),
                localizer.getMessage("interaction.clickable_command.default_highlight")),

            text.newClickableTextArgument(
                localizer.getMessage("constants.open_status.closed"), TextType.CLICKABLE,
                "/animatedarchitecture SetOpenStatus " + localizer.getMessage("constants.open_status.closed"),
                localizer.getMessage("interaction.clickable_command.default_highlight")));
    }

    protected Text setOpenDirectionTextSupplier(Text text)
    {
        text.append(localizer.getMessage("creator.base.set_open_direction") + "\n", TextType.INFO,
                    text.newTextArgument(localizer.getStructureType(getStructureType()), TextType.HIGHLIGHT));

        getValidOpenDirections().stream().map(dir -> localizer.getMessage(dir.getLocalizationKey())).sorted().forEach(
            dir -> text.appendClickableText(
                dir + "\n", TextType.CLICKABLE,
                "/animatedarchitecture SetOpenDirection " + dir,
                localizer.getMessage("interaction.clickable_command.default_highlight")));

        return text;
    }

    private Text confirmPriceTextSupplier(Text text)
    {
        return text.append(
            localizer.getMessage("creator.base.confirm_structure_price"), TextType.INFO,

            text.newTextArgument(localizer.getStructureType(getStructureType()), TextType.INFO),
            text.newTextArgument(DECIMAL_FORMAT.format(getPrice().orElse(0)), TextType.HIGHLIGHT),

            text.newClickableTextArgument(
                localizer.getMessage("creator.base.confirm_structure_price.confirm"), TextType.CLICKABLE_CONFIRM,
                "/animatedarchitecture confirm",
                localizer.getMessage("interaction.clickable_command.default_highlight")),

            text.newClickableTextArgument(
                localizer.getMessage("creator.base.confirm_structure_price.refuse"), TextType.CLICKABLE_REFUSE,
                "/animatedarchitecture cancel",
                localizer.getMessage("interaction.clickable_command.default_highlight"))
        );
    }

    /**
     * Represents a synchronized wrapper for {@link DecimalFormat}.
     *
     * @author Pim
     */
    private static final class MyDecimalFormat
    {
        private final DecimalFormat decimalFormat;

        MyDecimalFormat()
        {
            decimalFormat = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ROOT));
            decimalFormat.setMaximumFractionDigits(2);
        }

        /**
         * Formats a double number as a String.
         *
         * @param number
         *     The double number to format
         * @return The String representation of the provided double value.
         *
         * @throws ArithmeticException
         *     If rounding is needed with rounding mode being set to RoundingMode.UNNECESSARY.
         */
        public synchronized String format(double number)
        {
            return decimalFormat.format(number);
        }
    }
}
