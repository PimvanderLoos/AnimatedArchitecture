package nl.pim16aap2.animatedarchitecture.core.tooluser.creator;

import lombok.Setter;
import lombok.experimental.Accessors;
import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.animation.StructureActivityManager;
import nl.pim16aap2.animatedarchitecture.core.api.IAnimatedArchitectureToolUtil;
import nl.pim16aap2.animatedarchitecture.core.api.IEconomyManager;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.SetOpenDirectionDelayed;
import nl.pim16aap2.animatedarchitecture.core.localization.PersonalizedLocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.LimitsManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
import nl.pim16aap2.animatedarchitecture.core.text.Text;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Procedure;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.AsyncStepExecutor;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutor;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorBoolean;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorLocation;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorOpenDirection;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutorString;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.StringUtil;
import nl.pim16aap2.animatedarchitecture.core.util.functional.CheckedFunction;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.testing.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.assertThatMessageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(1)
public class CreatorTest
{
    public static final Vector3Di DEFAULT_MIN = new Vector3Di(10, 20, 30);
    public static final Vector3Di DEFAULT_MAX = new Vector3Di(40, 50, 60);
    public static final Cuboid DEFAULT_CUBOID = new Cuboid(DEFAULT_MIN, DEFAULT_MAX);

    private static final List<Property<?>> PROPERTIES = List.of(Property.OPEN_STATUS, Property.ROTATION_POINT);

    private ToolUser.Context context;

    @Mock
    private StructureType structureType;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer player;

    @Mock
    private IEconomyManager economyManager;

    @Mock
    private LimitsManager limitsManager;

    @Mock
    private CommandFactory commandFactory;

    @Mock
    private IProtectionHookManager protectionHookManager;

    @Mock
    private PersonalizedLocalizer personalizedLocalizer;

    @BeforeEach
    void init()
    {
        when(structureType.getLocalizationKey()).thenReturn("StructureType");
        when(structureType.getProperties()).thenReturn(PROPERTIES);

        when(personalizedLocalizer.getMessage(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        final var assistedStepFactory = mock(Step.Factory.IFactory.class);

        when(assistedStepFactory
            .stepName(any(), anyString()))
            .thenAnswer(invocation -> new Step.Factory(invocation.getArgument(0), invocation.getArgument(1)));

        final var structureAnimationRequestBuilder = mock(StructureAnimationRequestBuilder.class);
        when(structureAnimationRequestBuilder
            .builder())
            .thenReturn(mock(StructureAnimationRequestBuilder.IBuilderStructure.class));

        context = new ToolUser.Context(
            mock(StructureBuilder.class),
            mock(ToolUserManager.class),
            mock(DatabaseManager.class),
            limitsManager,
            economyManager,
            protectionHookManager,
            mock(IAnimatedArchitectureToolUtil.class),
            structureAnimationRequestBuilder,
            mock(StructureActivityManager.class),
            commandFactory,
            assistedStepFactory
        );
    }

    @Test
    void testInvalidNameInput()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newCreatorFactory()
            .steps(
                newStepSupplier(creator0 -> new StepExecutorString(creator0::completeNamingStep)),
                newDefaultStep()
            )
            .create();

        final String invalidName = "123";
        // Verify the name is indeed invalid according to the latest implementation.
        assertFalse(StringUtil.isValidStructureName(invalidName));

        assertFalse(creator.handleInput(invalidName).join());

        assertThatMessageable(player)
            .sentErrorMessage("creator.base.error.invalid_name")
            .withArgs(invalidName, "StructureType");

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getName());
        assertFalse(creator.playerHasTool());
    }

    @Test
    void testValidNameInput()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newCreatorFactory()
            .steps(
                newStepSupplier(creator0 -> new StepExecutorString(creator0::completeNamingStep)),
                newDefaultStep()
            )
            .create();

        final String validName = "ValidName";
        // Verify the name is indeed valid according to the latest implementation.
        assertTrue(StringUtil.isValidStructureName(validName));

        assertTrue(creator.handleInput(validName).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertEquals(validName, creator.getName());
        assertTrue(creator.playerHasTool());
    }

    @Test
    void testWorldMismatch()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newCreatorFactory(1)
            .mockedWorld()
            .create();

        assertFalse(creator.verifyWorldMatch(UnitTestUtil.getWorld()));
        assertThatMessageable(player).sentErrorMessage("creator.base.error.world_mismatch");
    }

    @Test
    void testWorldMatch()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newCreatorFactory(1)
            .mockedWorld()
            .create();

        assertTrue(creator.verifyWorldMatch(Objects.requireNonNull(creator.getWorld())));

        verify(creator.getPlayer(), never())
            .sendSuccess(eq("creator.base.error.world_mismatch"), any(Text.ArgumentCreator[].class));
    }

    @Test
    void testFirstLocationNoAccessToLocation()
    {
        locationBlockedByProtectionHook();
        UnitTestUtil.initMessageable(player);
        final var creator = newFirstLocationCreator();

        // No access to location
        assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MIN, creator.getWorld())).join());

        assertThatMessageable(player).sentErrorMessage("tool_user.base.error.no_permission_for_location");

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getFirstPos());
        assertNull(creator.getWorld());
    }

    @Test
    void testFirstLocationSuccess()
    {
        allowedByProtectionHooks();
        UnitTestUtil.initMessageable(player);
        final var creator = newFirstLocationCreator();

        final var location = UnitTestUtil.getLocation(DEFAULT_MIN.toDouble().add(0.5F), creator.getWorld());

        assertTrue(creator.handleInput(location).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertEquals(DEFAULT_MIN, creator.getFirstPos());
        assertEquals(creator.getWorld(), location.getWorld());
    }

    @Test
    void testSecondLocationNoAccessToLocation()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newSecondLocationCreator();

        locationBlockedByProtectionHook();
        when(protectionHookManager.canBreakBlocksInCuboid(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));

        assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        assertThatMessageable(player).sentErrorMessage("tool_user.base.error.no_permission_for_location");

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getCuboid());
    }

    @Test
    void testSecondLocationNoAccessToCuboid()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newSecondLocationCreator();

        when(protectionHookManager.canBreakBlock(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));
        cuboidBlockedByProtectionHook();

        assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        assertThatMessageable(player).sentErrorMessage("tool_user.base.error.no_permission_for_location");

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getCuboid());
    }

    @Test
    void testSecondLocationTooBig()
    {
        allowedByProtectionHooks();
        UnitTestUtil.initMessageable(player);
        final var creator = newSecondLocationCreator();

        when(protectionHookManager.canBreakBlocksInCuboid(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));

        final int limit = DEFAULT_CUBOID.getVolume() - 1;
        when(limitsManager.getLimit(any(), any()))
            .thenReturn(OptionalInt.of(limit));

        // Not allowed, because the selected area is too big.
        assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        assertThatMessageable(player)
            .sentErrorMessage("creator.base.error.area_too_big")
            .withArgs("StructureType", DEFAULT_CUBOID.getVolume(), limit);

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getCuboid());
    }

    @Test
    void testSecondLocationSuccess()
    {
        allowedByProtectionHooks();
        UnitTestUtil.initMessageable(player);
        final var creator = newSecondLocationCreator();

        when(protectionHookManager.canBreakBlocksInCuboid(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));

        when(limitsManager.getLimit(any(), any()))
            .thenReturn(OptionalInt.of(DEFAULT_CUBOID.getVolume()));

        assertTrue(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertEquals(DEFAULT_CUBOID, creator.getCuboid());
    }

    @Test
    void testSecondLocationSuccessNoLimits()
    {
        UnitTestUtil.initMessageable(player);
        allowedByProtectionHooks();
        final var creator = newSecondLocationCreator();

        when(limitsManager.getLimit(any(), any())).thenReturn(OptionalInt.empty());

        assertTrue(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertEquals(DEFAULT_CUBOID, creator.getCuboid());
    }

    @Test
    void testConfirmPriceCancel()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newConfirmPriceCreator();

        // The result is true because the input 'false' was handled successfully.
        assertTrue(creator.handleInput(false).join());

        assertThatMessageable(player).sentErrorMessage("creator.base.error.creation_cancelled");

        assertEquals(1, creator.getStepsCompleted());
        assertFalse(creator.isActive());
    }

    @Test
    void testConfirmPriceNoEconomy()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newConfirmPriceCreator();

        when(economyManager.isEconomyEnabled()).thenReturn(false);

        assertTrue(creator.handleInput(true).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
    }

    @Test
    void testConfirmPriceInsufficientFunds()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newConfirmPriceCreator();

        when(economyManager.isEconomyEnabled()).thenReturn(true);
        when(economyManager.getPrice(any(), anyInt())).thenReturn(OptionalDouble.empty());
        when(economyManager.buyStructure(any(), any(), any(), anyInt()))
            .thenReturn(false);

        assertTrue(creator.handleInput(true).join());

        assertThatMessageable(player)
            .sentErrorMessage("creator.base.error.insufficient_funds")
            .withArgs("StructureType", 0D);

        assertEquals(1, creator.getStepsCompleted());
        assertFalse(creator.isActive());
    }

    @Test
    void testConfirmPriceSuccess()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newConfirmPriceCreator();

        when(economyManager.isEconomyEnabled()).thenReturn(true);
        when(economyManager.getPrice(any(), anyInt())).thenReturn(OptionalDouble.of(1));
        when(economyManager.buyStructure(any(), any(), any(), anyInt()))
            .thenReturn(true);

        assertTrue(creator.handleInput(true).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
    }

    @Test
    void testConfirmPriceSuccessNoPrice()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newConfirmPriceCreator();

        when(economyManager.isEconomyEnabled()).thenReturn(true);
        when(economyManager.getPrice(any(), anyInt())).thenReturn(OptionalDouble.empty());
        when(economyManager.buyStructure(any(), any(), any(), anyInt()))
            .thenReturn(true);

        assertTrue(creator.handleInput(true).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
    }

    @Test
    void testMovementDirectionStepInvalid()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newMovementDirectionCreator();
        final var direction = MovementDirection.NORTH;

        when(structureType.getValidMovementDirections())
            .thenReturn(EnumSet.of(MovementDirection.EAST, MovementDirection.WEST));

        final var setDirectionDelayed = mock(SetOpenDirectionDelayed.class);
        when(commandFactory.getSetOpenDirectionDelayed())
            .thenReturn(setDirectionDelayed);
        when(setDirectionDelayed.runDelayed(any(), any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        assertFalse(creator.handleInput(direction).join());

        assertThatMessageable(player)
            .sentErrorMessage("creator.base.error.invalid_option")
            .withArgs(direction.getLocalizationKey());

        Mockito.verify(setDirectionDelayed)
            .runDelayed(any(), any(), any(), any());

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getMovementDirection());
    }

    @Test
    void testMovementDirectionStepSuccess()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newMovementDirectionCreator();
        final var direction = MovementDirection.EAST;

        when(structureType.getValidMovementDirections())
            .thenReturn(EnumSet.of(MovementDirection.EAST, MovementDirection.WEST));

        assertTrue(creator.handleInput(direction).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertEquals(MovementDirection.EAST, creator.getMovementDirection());

        verify(creator.getPlayer(), never())
            .sendSuccess(eq("creator.base.error.invalid_option"), any(Text.ArgumentCreator[].class));
    }

    @Test
    void testGetPriceNoEconomy()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newCreatorFactory()
            .steps(newDefaultStep())
            .defaultCuboid()
            .create();

        when(economyManager.isEconomyEnabled()).thenReturn(false);
        assertTrue(creator.getPrice().isEmpty());
    }

    @Test
    void testGetPrice()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newCreatorFactory()
            .steps(newDefaultStep())
            .defaultCuboid()
            .create();

        when(economyManager.isEconomyEnabled()).thenReturn(true);
        when(economyManager.getPrice(any(), anyInt()))
            .thenAnswer(invocation -> OptionalDouble.of(invocation.getArgument(1, Integer.class).doubleValue()));

        final OptionalDouble price = creator.getPrice();
        assertTrue(price.isPresent());
        assertEquals(DEFAULT_CUBOID.getVolume(), price.getAsDouble());
    }

    @Test
    void testCompleteSetPowerBlockStepWorldMismatch()
    {
        allowedByProtectionHooks();
        UnitTestUtil.initMessageable(player);
        final var creator = newPowerBlockCreator();

        assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX.add(10))).join());

        assertThatMessageable(player)
            .sentErrorMessage("creator.base.error.world_mismatch");
    }

    @Test
    void testCompleteSetPowerBlockStepLocationNotAllowed()
    {
        locationBlockedByProtectionHook();
        UnitTestUtil.initMessageable(player);
        final var creator = newPowerBlockCreator();

        assertFalse(
            creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX.add(10), creator.getWorld())).join());

        assertThatMessageable(player)
            .sentErrorMessage("tool_user.base.error.no_permission_for_location");

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getPowerBlock());
    }

    @Test
    void testCompleteSetPowerBlockStepInsideStructure()
    {
        allowedByProtectionHooks();
        UnitTestUtil.initMessageable(player);
        final var creator = newPowerBlockCreator();

        final ILocation location = UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld());

        assertFalse(creator.handleInput(location).join());

        assertThatMessageable(player)
            .sentErrorMessage("creator.base.error.powerblock_inside_structure")
            .withArgs("StructureType");

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getPowerBlock());
    }

    @Test
    void testCompleteSetPowerBlockStepTooFar()
    {
        allowedByProtectionHooks();
        UnitTestUtil.initMessageable(player);
        final var creator = newPowerBlockCreator();

        final int distance = 10;
        final ILocation location = UnitTestUtil.getLocation(DEFAULT_MAX.add(distance), creator.getWorld());

        final int lowLimit = distance - 1;
        when(limitsManager.getLimit(any(), any())).thenReturn(OptionalInt.of(lowLimit));

        assertFalse(creator.handleInput(location).join());

        assertThatMessageable(player)
            .sentErrorMessage("creator.base.error.powerblock_too_far")
            .withArgs("StructureType", distance, lowLimit);

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getPowerBlock());
    }

    @Test
    void testCompleteSetPowerBlockStepSuccess()
    {
        allowedByProtectionHooks();
        UnitTestUtil.initMessageable(player);
        final var creator = newPowerBlockCreator();

        final int distance = 10;
        final Vector3Di position = DEFAULT_MAX.add(distance);
        final ILocation location = UnitTestUtil.getLocation(position, creator.getWorld());

        when(limitsManager.getLimit(any(), any())).thenReturn(OptionalInt.of(distance));

        assertTrue(creator.handleInput(location).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertEquals(position, creator.getPowerBlock());
    }

    @Test
    void testCompleteSetPowerBlockStepSuccessNoLimits()
    {
        allowedByProtectionHooks();
        UnitTestUtil.initMessageable(player);
        final var creator = newPowerBlockCreator();

        final Vector3Di position = DEFAULT_MAX.add(10);

        when(limitsManager.getLimit(any(), any())).thenReturn(OptionalInt.empty());

        assertTrue(creator.handleInput(UnitTestUtil.getLocation(position, creator.getWorld())).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertEquals(position, creator.getPowerBlock());
    }

    @Test
    void testCompleteSetRotationPointStepWorldMismatch()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newSetRotationPointCreator();

        assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX)).join());

        assertThatMessageable(player)
            .sentErrorMessage("creator.base.error.world_mismatch");

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getProperty(Property.ROTATION_POINT));
    }

    @Test
    void testCompleteSetRotationPointStepPointInvalid()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newSetRotationPointCreator();

        assertFalse(
            creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX.add(2, 2, 2), creator.getWorld())).join());

        assertThatMessageable(player)
            .sentErrorMessage("creator.base.error.invalid_rotation_point");

        assertEquals(0, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertNull(creator.getProperty(Property.ROTATION_POINT));
    }

    @Test
    void testCompleteSetRotationPointStepSuccess()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newSetRotationPointCreator();

        assertTrue(
            creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertEquals(DEFAULT_MAX, creator.getProperty(Property.ROTATION_POINT));
    }

    @Test
    void testUpdateProcess()
    {
        UnitTestUtil.initMessageable(player);
        final var creator = newUpdateCreator();

        when(structureType.getValidMovementDirections())
            .thenReturn(EnumSet.of(MovementDirection.EAST, MovementDirection.WEST));

        assertFalse(creator.isProcessIsUpdatable());
        assertTrue(creator.handleInput(MovementDirection.EAST).join());
        // The process is now updatable because the preview step supports it.
        assertTrue(creator.isProcessIsUpdatable());

        // Make sure that we cannot update the step after it has been completed regardless of the updatable flag.
        assertThrows(
            CompletionException.class,
            () -> creator.handleInput(MovementDirection.WEST).join()
        );

        assertEquals(1, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertEquals(MovementDirection.EAST, creator.getMovementDirection());

        assertTrue(creator.update("OPEN_DIRECTION", null).join());
        assertTrue(creator.handleInput(MovementDirection.WEST).join());

        assertEquals(2, creator.getStepsCompleted());
        assertTrue(creator.isActive());
        assertEquals(MovementDirection.WEST, creator.getMovementDirection());
        assertFalse(creator.getProcedure().hasNextStep());
    }

    private CreatorImpl newUpdateCreator()
    {
        final CheckedFunction<Creator, Step, InstantiationException> openDirectionStep =
            creator -> context
                .getStepFactory()
                .stepName(personalizedLocalizer, "OPEN_DIRECTION")
                .stepExecutor(new StepExecutorOpenDirection(creator::completeSetOpenDirStep))
                .textSupplier(text -> text.append("OPEN_DIRECTION"))
                .updatable(true)
                .construct();

        final CheckedFunction<Creator, Step, InstantiationException> previewStep =
            creator -> context
                .getStepFactory()
                .stepName(personalizedLocalizer, "PREVIEW")
                .stepPreparation(creator::prepareReviewResult)
                .stepExecutor(new StepExecutorBoolean(ignored -> true))
                .textSupplier(text -> text.append("PREVIEW"))
                .construct();

        return newCreatorFactory()
            .steps(
                openDirectionStep,
                previewStep
            )
            .create();
    }

    private CreatorImpl newFirstLocationCreator()
    {
        return newCreatorFactory()
            .steps(
                newStepSupplier(creator0 -> new AsyncStepExecutor<>(ILocation.class, creator0::provideFirstPos)),
                newDefaultStep())
            .create();
    }

    private CreatorImpl newSecondLocationCreator()
    {
        return newCreatorFactory()
            .steps(
                newStepSupplier(creator0 -> new AsyncStepExecutor<>(ILocation.class, creator0::provideSecondPos)),
                newDefaultStep())
            .mockedWorld()
            .firstPos(DEFAULT_MIN)
            .create();
    }

    private CreatorImpl newPowerBlockCreator()
    {
        return newCreatorFactory()
            .steps(
                newStepSupplier(
                    creator0 -> new AsyncStepExecutor<>(ILocation.class, creator0::completeSetPowerBlockStep)),
                newDefaultStep())
            .mockedWorld()
            .firstPos(DEFAULT_MIN)
            .defaultCuboid()
            .create();
    }

    private CreatorImpl newConfirmPriceCreator()
    {
        return newCreatorFactory()
            .steps(
                newStepSupplier(creator0 -> new StepExecutorBoolean(creator0::confirmPrice)),
                newDefaultStep())
            .defaultCuboid()
            .mockedWorld()
            .create();
    }

    private CreatorImpl newMovementDirectionCreator()
    {
        return newCreatorFactory()
            .steps(
                newStepSupplier(creator0 -> new StepExecutorOpenDirection(creator0::completeSetOpenDirStep)),
                newDefaultStep())
            .defaultCuboid()
            .mockedWorld()
            .create();
    }

    private CreatorImpl newSetRotationPointCreator()
    {
        return newCreatorFactory()
            .steps(
                newStepSupplier(creator0 -> new StepExecutorLocation(creator0::completeSetRotationPointStep)),
                newDefaultStep())
            .defaultCuboid()
            .mockedWorld()
            .create();
    }

    private void allowedByProtectionHooks()
    {
        when(protectionHookManager.canBreakBlock(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));
        when(protectionHookManager.canBreakBlocksInCuboid(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));
    }

    private void locationBlockedByProtectionHook()
    {
        when(protectionHookManager.canBreakBlock(any(), any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.ERROR));
    }

    private void cuboidBlockedByProtectionHook()
    {
        when(protectionHookManager.canBreakBlocksInCuboid(any(), any(), any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.ERROR));
    }

    /**
     * Creates a function that creates a new step with the given executor for a Creator that will be supplied later.
     *
     * @param executorFunction
     *     The function that creates the executor.
     * @return The new function.
     */
    private Function<Creator, Step> newStepSupplier(Function<Creator, StepExecutor> executorFunction)
    {
        return creator -> newStep(executorFunction.apply(creator));
    }

    /**
     * Creates a new step with the given executor.
     * <p>
     * See {@link #newDefaultStepFactory(StepExecutor)}.
     *
     * @param executor
     *     The executor to use.
     * @return The new step.
     */
    private Step newStep(StepExecutor executor)
    {
        try
        {
            return newDefaultStepFactory(executor).construct();
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new default step factory.
     * <p>
     * All the required fields are defined either using dummy values or mocks.
     * <p>
     * The step is named "DefaultStep_" followed by the current nano time.
     *
     * @return The new default step factory.
     */
    private Step.Factory newDefaultStepFactory(@Nullable StepExecutor stepExecutor)
    {
        final String stepName = "DefaultStep_" + System.nanoTime();
        return context
            .getStepFactory()
            .stepName(personalizedLocalizer, stepName)
            .stepExecutor(Objects.requireNonNullElseGet(stepExecutor, () -> mock(StepExecutor.class)))
            .textSupplier(text -> text.append(stepName));
    }

    /**
     * See {@link #newDefaultStepFactory(StepExecutor)}.
     * <p>
     * Uses a mock for the step executor.
     *
     * @return The new default step factory.
     */
    private Step.Factory newDefaultStepFactory()
    {
        return newDefaultStepFactory(null);
    }

    /**
     * Creates a new default step.
     * <p>
     * See {@link #newDefaultStepFactory()}.
     *
     * @return The new default step.
     */
    private Step newDefaultStep()
    {
        try
        {
            return newDefaultStepFactory().construct();
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a new factory for a Creator.
     *
     * @return The new factory.
     */
    private CreatorImplFactory newCreatorFactory()
    {
        return new CreatorImplFactory(context, player, structureType);
    }

    /**
     * Creates a new factory for a Creator with the given number of default steps.
     * <p>
     * See {@link #newCreatorFactory()} and {@link #newDefaultStep()}.
     *
     * @param defaultSteps
     *     The number of default steps to create.
     * @return The new factory.
     */
    private CreatorImplFactory newCreatorFactory(int defaultSteps)
    {
        final var factory = newCreatorFactory();
        final List<Step> steps = new ArrayList<>(defaultSteps);
        for (int i = 0; i < defaultSteps; i++)
            steps.add(newDefaultStep());
        return factory.steps(steps);
    }

    @Setter
    @Accessors(fluent = true, chain = true)
    private static final class CreatorImplFactory
    {
        private final StructureType structureType;
        private final ToolUser.Context context;
        private final IPlayer player;

        private @Nullable List<?> steps;
        private @Nullable IWorld world;
        private @Nullable Vector3Di firstPos;
        private @Nullable Cuboid cuboid;
        private @Nullable String name;

        public CreatorImplFactory(
            ToolUser.Context context,
            IPlayer player,
            StructureType structureType)
        {
            this.structureType = structureType;
            this.context = context;
            this.player = player;
        }

        public CreatorImplFactory defaultCuboid()
        {
            return cuboid(DEFAULT_CUBOID);
        }

        public CreatorImplFactory mockedWorld()
        {
            return world(UnitTestUtil.getWorld());
        }

        /**
         * Sets the steps or step suppliers for the Creator.
         * <p>
         * If the step is a {@link Step}, it is added directly.
         * <p>
         * If the step is a {@link Function}, it should be a function that takes a {@link Creator} and returns a
         * {@link Step}. The function will be called with the Creator as the argument.
         *
         * @param steps
         *     The steps or step suppliers to use.
         * @return This factory.
         */
        public CreatorImplFactory steps(List<?> steps)
        {
            this.steps = steps;
            return this;
        }

        /**
         * Sets the steps or step suppliers for the Creator.
         * <p>
         * If the step is a {@link Step}, it is added directly.
         * <p>
         * If the step is a {@link Function}, it should be a function that takes a {@link Creator} and returns a
         * {@link Step}. The function will be called with the Creator as the argument.
         *
         * @param steps
         *     The steps or step suppliers to use.
         * @return This factory.
         */
        public CreatorImplFactory steps(Object... steps)
        {
            if (steps.length == 1 &&
                steps[0] instanceof List<?> lst &&
                !lst.isEmpty() &&
                lst.getFirst() instanceof Step)
                return steps(lst);

            return steps(List.of(steps));
        }

        public CreatorImpl create()
        {
            if (steps == null)
                throw new IllegalStateException("No steps or step suppliers defined!");

            final var ret = new CreatorImpl(context, player, structureType, name);

            final List<Step> realSteps = new ArrayList<>(steps.size());
            for (final Object step : steps)
            {
                switch (step)
                {
                    case Step step1 -> realSteps.add(step1);
                    //noinspection rawtypes
                    case Function function ->
                        //noinspection unchecked
                        realSteps.add(((Function<Creator, Step>) function).apply(ret));
                    //noinspection rawtypes
                    case CheckedFunction checkedFunction ->
                    {
                        try
                        {
                            //noinspection unchecked
                            realSteps.add(((CheckedFunction<Creator, Step, ?>) checkedFunction).apply(ret));
                        }
                        catch (Exception e)
                        {
                            throw new RuntimeException("Failed to create step! Previous steps: " + realSteps, e);
                        }
                    }
                    case null, default ->
                        throw new IllegalArgumentException("Invalid step type: " + step.getClass().getSimpleName());
                }
            }

            ret.steps = realSteps;

            if (world != null)
                ret.setWorld(world);

            if (firstPos != null)
                ret.setFirstPos(firstPos);

            if (cuboid != null)
                ret.setCuboid(cuboid);

            ret.init();

            return ret;
        }
    }

    private static final class CreatorImpl extends Creator
    {
        private List<Step> steps;
        private Procedure procedure;

        public CreatorImpl(
            ToolUser.Context context,
            IPlayer player,
            StructureType structureType,
            @Nullable String name)
        {
            super(context, structureType, player, name);
        }

        @Override
        protected synchronized void init()
        {
            Objects.requireNonNull(steps, "Steps must be defined!");
            super.init();
            this.procedure = findProcedure();
        }

        @Override
        protected void giveTool()
        {
            // Do nothing
        }

        @Override
        protected Structure constructStructure()
        {
            throw new UnsupportedOperationException("No implemented!");
        }

        @Override
        protected List<Step> generateSteps()
        {
            return this.steps;
        }

        @Override
        protected void showPreview()
        {
            // Do nothing
        }

        public Procedure getProcedure()
        {
            return procedure;
        }

        private Procedure findProcedure()
        {
            final var field = ReflectionUtil.getField(ToolUser.class, "procedure");
            field.setAccessible(true);
            try
            {
                return (Procedure) field.get(this);
            }
            catch (IllegalAccessException | ClassCastException e)
            {
                throw new RuntimeException("Failed to obtain procedure!", e);
            }
        }

        /**
         * Shortcut for getting the number of steps completed.
         *
         * @return The number of steps completed in the procedure.
         */
        public int getStepsCompleted()
        {
            return getProcedure().getStepsCompleted();
        }
    }
}
