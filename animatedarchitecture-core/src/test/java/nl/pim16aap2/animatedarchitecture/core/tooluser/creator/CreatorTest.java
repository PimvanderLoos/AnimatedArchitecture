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
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.CommandFactory;
import nl.pim16aap2.animatedarchitecture.core.commands.SetOpenDirectionDelayed;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.managers.LimitsManager;
import nl.pim16aap2.animatedarchitecture.core.managers.ToolUserManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBaseBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.structures.properties.Property;
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
import org.junit.jupiter.api.Assertions;
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

    @BeforeEach
    void init()
    {
        Mockito.when(structureType.getLocalizationKey()).thenReturn("StructureType");
        Mockito.when(structureType.getProperties()).thenReturn(PROPERTIES);

        final ILocalizer localizer = UnitTestUtil.initLocalizer();
        final var assistedStepFactory = Mockito.mock(Step.Factory.IFactory.class);
        //noinspection deprecation
        Mockito.when(assistedStepFactory.stepName(Mockito.anyString()))
            .thenAnswer(invocation -> new Step.Factory(localizer, invocation.getArgument(0, String.class)));

        final var structureAnimationRequestBuilder = Mockito.mock(StructureAnimationRequestBuilder.class);
        Mockito.when(structureAnimationRequestBuilder.builder())
            .thenReturn(Mockito.mock(StructureAnimationRequestBuilder.IBuilderStructure.class));

        context = new ToolUser.Context(
            Mockito.mock(StructureBaseBuilder.class),
            localizer,
            ITextFactory.getSimpleTextFactory(),
            Mockito.mock(ToolUserManager.class),
            Mockito.mock(DatabaseManager.class),
            limitsManager,
            economyManager,
            protectionHookManager,
            Mockito.mock(IAnimatedArchitectureToolUtil.class),
            structureAnimationRequestBuilder,
            Mockito.mock(StructureActivityManager.class),
            commandFactory,
            assistedStepFactory
        );
    }

    @Test
    void testInvalidNameInput()
    {
        final var creator = newCreatorFactory()
            .steps(
                newStepSupplier(creator0 -> new StepExecutorString(creator0::completeNamingStep)),
                newDefaultStep()
            )
            .create();

        final String invalidName = "123";
        // Verify the name is indeed invalid according to the latest implementation.
        Assertions.assertFalse(StringUtil.isValidStructureName(invalidName));

        Assertions.assertFalse(creator.handleInput(invalidName).join());

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.invalid_name"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getName());
        Assertions.assertFalse(creator.playerHasTool());
    }

    @Test
    void testValidNameInput()
    {
        final var creator = newCreatorFactory()
            .steps(
                newStepSupplier(creator0 -> new StepExecutorString(creator0::completeNamingStep)),
                newDefaultStep()
            )
            .create();

        final String validName = "ValidName";
        // Verify the name is indeed valid according to the latest implementation.
        Assertions.assertTrue(StringUtil.isValidStructureName(validName));

        Assertions.assertTrue(creator.handleInput(validName).join());

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertEquals(validName, creator.getName());
        Assertions.assertTrue(creator.playerHasTool());
    }

    @Test
    void testWorldMismatch()
    {
        final var creator = newCreatorFactory(1)
            .mockedWorld()
            .create();

        Assertions.assertFalse(creator.verifyWorldMatch(UnitTestUtil.getWorld()));
        Mockito.verify(player)
            .sendMessage(UnitTestUtil.textArgumentMatcher("creator.base.error.world_mismatch"));
    }

    @Test
    void testWorldMatch()
    {
        final var creator = newCreatorFactory(1)
            .mockedWorld()
            .create();

        Assertions.assertTrue(creator.verifyWorldMatch(Objects.requireNonNull(creator.getWorld())));
        Mockito.verify(player, Mockito.never())
            .sendMessage(UnitTestUtil.textArgumentMatcher("creator.base.error.world_mismatch"));
    }

    @Test
    void testFirstLocationNoAccessToLocation()
    {
        locationBlockedByProtectionHook();
        final var creator = newFirstLocationCreator();

        // No access to location
        Assertions.assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MIN, creator.getWorld())).join());

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("tool_user.base.error.no_permission_for_location"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getFirstPos());
        Assertions.assertNull(creator.getWorld());
    }

    @Test
    void testFirstLocationSuccess()
    {
        allowedByProtectionHooks();
        final var creator = newFirstLocationCreator();

        final var location = UnitTestUtil.getLocation(DEFAULT_MIN.toDouble().add(0.5F), creator.getWorld());

        Assertions.assertTrue(creator.handleInput(location).join());

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertEquals(DEFAULT_MIN, creator.getFirstPos());
        Assertions.assertEquals(creator.getWorld(), location.getWorld());
    }

    @Test
    void testSecondLocationNoAccessToLocation()
    {
        final var creator = newSecondLocationCreator();

        locationBlockedByProtectionHook();
        Mockito.when(protectionHookManager.canBreakBlocksInCuboid(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));

        Assertions.assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("tool_user.base.error.no_permission_for_location"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getCuboid());
    }

    @Test
    void testSecondLocationNoAccessToCuboid()
    {
        final var creator = newSecondLocationCreator();

        Mockito.when(protectionHookManager.canBreakBlock(Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));
        cuboidBlockedByProtectionHook();

        Assertions.assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("tool_user.base.error.no_permission_for_location"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getCuboid());
    }

    @Test
    void testSecondLocationTooBig()
    {
        allowedByProtectionHooks();
        final var creator = newSecondLocationCreator();

        Mockito.when(protectionHookManager.canBreakBlocksInCuboid(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));

        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any()))
            .thenReturn(OptionalInt.of(DEFAULT_CUBOID.getVolume() - 1));

        // Not allowed, because the selected area is too big.
        Assertions.assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.area_too_big"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getCuboid());
    }

    @Test
    void testSecondLocationSuccess()
    {
        allowedByProtectionHooks();
        final var creator = newSecondLocationCreator();

        Mockito.when(protectionHookManager.canBreakBlocksInCuboid(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));

        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any()))
            .thenReturn(OptionalInt.of(DEFAULT_CUBOID.getVolume()));

        Assertions.assertTrue(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertEquals(DEFAULT_CUBOID, creator.getCuboid());
    }

    @Test
    void testSecondLocationSuccessNoLimits()
    {
        allowedByProtectionHooks();
        final var creator = newSecondLocationCreator();

        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any())).thenReturn(OptionalInt.empty());

        Assertions.assertTrue(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertEquals(DEFAULT_CUBOID, creator.getCuboid());
    }

    @Test
    void testConfirmPriceCancel()
    {
        final var creator = newConfirmPriceCreator();

        // The result is true because the input 'false' was handled successfully.
        Assertions.assertTrue(creator.handleInput(false).join());

        Mockito.verify(player).sendMessage(UnitTestUtil.textArgumentMatcher("creator.base.error.creation_cancelled"));

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertFalse(creator.isActive());
    }

    @Test
    void testConfirmPriceNoEconomy()
    {
        final var creator = newConfirmPriceCreator();

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(false);

        Assertions.assertTrue(creator.handleInput(true).join());

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
    }

    @Test
    void testConfirmPriceInsufficientFunds()
    {
        final var creator = newConfirmPriceCreator();

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        Mockito.when(economyManager.getPrice(Mockito.any(), Mockito.anyInt())).thenReturn(OptionalDouble.empty());
        Mockito.when(economyManager.buyStructure(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt()))
            .thenReturn(false);

        Assertions.assertTrue(creator.handleInput(true).join());
        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.insufficient_funds"));

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertFalse(creator.isActive());
    }

    @Test
    void testConfirmPriceSuccess()
    {
        final var creator = newConfirmPriceCreator();

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        Mockito.when(economyManager.getPrice(Mockito.any(), Mockito.anyInt())).thenReturn(OptionalDouble.of(1));
        Mockito.when(economyManager.buyStructure(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt()))
            .thenReturn(true);

        Assertions.assertTrue(creator.handleInput(true).join());

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
    }

    @Test
    void testConfirmPriceSuccessNoPrice()
    {
        final var creator = newConfirmPriceCreator();

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        Mockito.when(economyManager.getPrice(Mockito.any(), Mockito.anyInt())).thenReturn(OptionalDouble.empty());
        Mockito.when(economyManager.buyStructure(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt()))
            .thenReturn(true);

        Assertions.assertTrue(creator.handleInput(true).join());

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
    }

    @Test
    void testMovementDirectionStepInvalid()
    {
        final var creator = newMovementDirectionCreator();

        Mockito.when(structureType.getValidMovementDirections())
            .thenReturn(EnumSet.of(MovementDirection.EAST, MovementDirection.WEST));

        final var setDirectionDelayed = Mockito.mock(SetOpenDirectionDelayed.class);
        Mockito.when(commandFactory.getSetOpenDirectionDelayed())
            .thenReturn(setDirectionDelayed);
        Mockito.when(setDirectionDelayed.runDelayed(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(null));

        Assertions.assertFalse(creator.handleInput(MovementDirection.NORTH).join());

        Mockito.verify(player).sendMessage(UnitTestUtil.textArgumentMatcher("creator.base.error.invalid_option"));

        Mockito.verify(setDirectionDelayed)
            .runDelayed(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getMovementDirection());
    }

    @Test
    void testMovementDirectionStepSuccess()
    {
        final var creator = newMovementDirectionCreator();

        Mockito.when(structureType.getValidMovementDirections())
            .thenReturn(EnumSet.of(MovementDirection.EAST, MovementDirection.WEST));

        Assertions.assertTrue(creator.handleInput(MovementDirection.EAST).join());

        Mockito.verify(player, Mockito.never())
            .sendMessage(UnitTestUtil.textArgumentMatcher("creator.base.error.invalid_option"));

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertEquals(MovementDirection.EAST, creator.getMovementDirection());
    }

    @Test
    void testGetPriceNoEconomy()
    {
        final var creator = newCreatorFactory()
            .steps(newDefaultStep())
            .defaultCuboid()
            .create();

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(false);
        Assertions.assertTrue(creator.getPrice().isEmpty());
    }

    @Test
    void testGetPrice()
    {
        final var creator = newCreatorFactory()
            .steps(newDefaultStep())
            .defaultCuboid()
            .create();

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        Mockito.when(economyManager.getPrice(Mockito.any(), Mockito.anyInt()))
            .thenAnswer(invocation -> OptionalDouble.of(invocation.getArgument(1, Integer.class).doubleValue()));

        final OptionalDouble price = creator.getPrice();
        Assertions.assertTrue(price.isPresent());
        Assertions.assertEquals(DEFAULT_CUBOID.getVolume(), price.getAsDouble());
    }

    @Test
    void testCompleteSetPowerBlockStepWorldMismatch()
    {
        allowedByProtectionHooks();
        final var creator = newPowerBlockCreator();

        Assertions.assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX.add(10))).join());

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.world_mismatch"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getPowerBlock());
    }

    @Test
    void testCompleteSetPowerBlockStepLocationNotAllowed()
    {
        locationBlockedByProtectionHook();
        final var creator = newPowerBlockCreator();

        Assertions.assertFalse(
            creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX.add(10), creator.getWorld())).join());

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("tool_user.base.error.no_permission_for_location"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getPowerBlock());
    }

    @Test
    void testCompleteSetPowerBlockStepInsideStructure()
    {
        allowedByProtectionHooks();
        final var creator = newPowerBlockCreator();

        final ILocation location = UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld());

        Assertions.assertFalse(creator.handleInput(location).join());

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.powerblock_inside_structure"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getPowerBlock());
    }

    @Test
    void testCompleteSetPowerBlockStepTooFar()
    {
        allowedByProtectionHooks();
        final var creator = newPowerBlockCreator();

        final int distance = 10;
        final ILocation location = UnitTestUtil.getLocation(DEFAULT_MAX.add(distance), creator.getWorld());

        final int lowLimit = distance - 1;
        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any())).thenReturn(OptionalInt.of(lowLimit));

        Assertions.assertFalse(creator.handleInput(location).join());

        Mockito.verify(player).sendMessage(UnitTestUtil.textArgumentMatcher("creator.base.error.powerblock_too_far"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getPowerBlock());
    }

    @Test
    void testCompleteSetPowerBlockStepSuccess()
    {
        allowedByProtectionHooks();
        final var creator = newPowerBlockCreator();

        final int distance = 10;
        final Vector3Di position = DEFAULT_MAX.add(distance);
        final ILocation location = UnitTestUtil.getLocation(position, creator.getWorld());

        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any())).thenReturn(OptionalInt.of(distance));

        Assertions.assertTrue(creator.handleInput(location).join());

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertEquals(position, creator.getPowerBlock());
    }

    @Test
    void testCompleteSetPowerBlockStepSuccessNoLimits()
    {
        allowedByProtectionHooks();
        final var creator = newPowerBlockCreator();

        final Vector3Di position = DEFAULT_MAX.add(10);

        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any())).thenReturn(OptionalInt.empty());

        Assertions.assertTrue(creator.handleInput(UnitTestUtil.getLocation(position, creator.getWorld())).join());

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertEquals(position, creator.getPowerBlock());
    }

    @Test
    void testCompleteSetRotationPointStepWorldMismatch()
    {
        final var creator = newSetRotationPointCreator();

        Assertions.assertFalse(creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX)).join());

        Mockito.verify(player).sendMessage(UnitTestUtil.textArgumentMatcher("creator.base.error.world_mismatch"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getProperty(Property.ROTATION_POINT));
    }

    @Test
    void testCompleteSetRotationPointStepPointInvalid()
    {
        final var creator = newSetRotationPointCreator();

        Assertions.assertFalse(
            creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX.add(2, 2, 2), creator.getWorld())).join());

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.invalid_rotation_point"));

        Assertions.assertEquals(0, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertNull(creator.getProperty(Property.ROTATION_POINT));
    }

    @Test
    void testCompleteSetRotationPointStepSuccess()
    {
        final var creator = newSetRotationPointCreator();

        Assertions.assertTrue(
            creator.handleInput(UnitTestUtil.getLocation(DEFAULT_MAX, creator.getWorld())).join());

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertEquals(DEFAULT_MAX, creator.getProperty(Property.ROTATION_POINT));
    }

    @Test
    void testUpdateProcess()
    {
        final var creator = newUpdateCreator();

        Mockito.when(structureType.getValidMovementDirections())
            .thenReturn(EnumSet.of(MovementDirection.EAST, MovementDirection.WEST));

        Assertions.assertFalse(creator.isProcessIsUpdatable());
        Assertions.assertTrue(creator.handleInput(MovementDirection.EAST).join());
        // The process is now updatable because the preview step supports it.
        Assertions.assertTrue(creator.isProcessIsUpdatable());

        // Make sure that we cannot update the step after it has been completed regardless of the updatable flag.
        Assertions.assertThrows(
            CompletionException.class,
            () -> creator.handleInput(MovementDirection.WEST).join()
        );

        Assertions.assertEquals(1, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertEquals(MovementDirection.EAST, creator.getMovementDirection());

        Assertions.assertTrue(creator.update("OPEN_DIRECTION", null).join());
        Assertions.assertTrue(creator.handleInput(MovementDirection.WEST).join());

        Assertions.assertEquals(2, creator.getStepsCompleted());
        Assertions.assertTrue(creator.isActive());
        Assertions.assertEquals(MovementDirection.WEST, creator.getMovementDirection());
        Assertions.assertFalse(creator.getProcedure().hasNextStep());
    }

    private CreatorImpl newUpdateCreator()
    {
        final CheckedFunction<Creator, Step, InstantiationException> openDirectionStep =
            creator -> context
                .getStepFactory()
                .stepName("OPEN_DIRECTION")
                .stepExecutor(new StepExecutorOpenDirection(creator::completeSetOpenDirStep))
                .textSupplier(text -> text.append("OPEN_DIRECTION"))
                .updatable(true)
                .construct();

        final CheckedFunction<Creator, Step, InstantiationException> previewStep =
            creator -> context
                .getStepFactory()
                .stepName("PREVIEW")
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
        Mockito.when(protectionHookManager.canBreakBlock(Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));
        Mockito.when(protectionHookManager.canBreakBlocksInCuboid(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));
    }

    private void locationBlockedByProtectionHook()
    {
        Mockito.when(protectionHookManager.canBreakBlock(Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.ERROR));
    }

    private void cuboidBlockedByProtectionHook()
    {
        Mockito.when(protectionHookManager.canBreakBlocksInCuboid(Mockito.any(), Mockito.any(), Mockito.any()))
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
            .stepName(stepName)
            .stepExecutor(Objects.requireNonNullElseGet(stepExecutor, () -> Mockito.mock(StepExecutor.class)))
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
        protected void init()
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
