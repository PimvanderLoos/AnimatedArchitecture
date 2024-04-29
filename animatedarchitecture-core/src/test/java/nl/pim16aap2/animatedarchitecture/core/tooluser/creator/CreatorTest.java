package nl.pim16aap2.animatedarchitecture.core.tooluser.creator;

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
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureAnimationRequestBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureBaseBuilder;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Procedure;
import nl.pim16aap2.animatedarchitecture.core.tooluser.Step;
import nl.pim16aap2.animatedarchitecture.core.tooluser.ToolUser;
import nl.pim16aap2.animatedarchitecture.core.tooluser.stepexecutor.StepExecutor;
import nl.pim16aap2.animatedarchitecture.core.util.Cuboid;
import nl.pim16aap2.animatedarchitecture.core.util.MovementDirection;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.testing.reflection.ReflectionUtil;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CreatorTest
{
//    private CreatorImpl creator;

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

    @BeforeEach
    void init()
    {
        Mockito.when(structureType.getLocalizationKey()).thenReturn("StructureType");

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);

        final IProtectionHookManager protectionHookManager = Mockito.mock(IProtectionHookManager.class);
        Mockito.when(protectionHookManager.canBreakBlock(Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(protectionHookManager.canBreakBlocksBetweenLocs(Mockito.any(),
                                                                     Mockito.any(), Mockito.any()))
               .thenReturn(Optional.empty());

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
    void testNameInput()
    {
        final Creator creator = newCreator();

        final String input = "1";
        // Numerical names are not allowed.
        Assertions.assertFalse(creator.completeNamingStep(input));
        Mockito.verify(player).sendMessage(UnitTestUtil.textArgumentMatcher("creator.base.error.invalid_name"));

        Assertions.assertTrue(creator.completeNamingStep("newDoor"));
        Mockito.verify(creator).giveTool();
    }

    @Test
    void testFirstLocation()
    {
        final Creator creator = newCreator();

        final ILocation loc = UnitTestUtil.getLocation(12.7, 128, 56.12);

        Mockito.doReturn(false).when(creator).playerHasAccessToLocation(Mockito.any());
        // No access to location
        Assertions.assertFalse(creator.provideFirstPos(loc));

        Mockito.doReturn(true).when(creator).playerHasAccessToLocation(Mockito.any());
        Assertions.assertTrue(creator.provideFirstPos(loc));
        Assertions.assertEquals(loc.getWorld(), creator.getWorld());
        Assertions.assertEquals(new Vector3Di(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                                creator.getFirstPos());
    }

    @Test
    void testWorldMatch()
    {
        final Creator creator = newCreator();

        final IWorld world = UnitTestUtil.getWorld();
        final String worldName = world.worldName();
        setField(creator, "world", world);

        final IWorld secondWorld = UnitTestUtil.getWorld();
        // Different world, so no match!
        Assertions.assertFalse(creator.verifyWorldMatch(Mockito.mock(IWorld.class)));

        Mockito.when(secondWorld.worldName()).thenReturn(worldName);
        // Same world name, so match!
        Assertions.assertTrue(creator.verifyWorldMatch(secondWorld));
    }

    @Test
    void testInit()
    {
        final Creator creator = newCreator();

        Assertions.assertDoesNotThrow(creator::init);
    }

    @Test
    void testSecondLocation()
    {
        final Creator creator = newCreator();

        Mockito.doReturn(false).when(creator).playerHasAccessToLocation(Mockito.any());

        final IWorld world = UnitTestUtil.getWorld();

        final Vector3Di vec1 = new Vector3Di(12, 128, 56);
        final Vector3Di vec2 = vec1.add(10, 10, 10);
        final Cuboid cuboid = new Cuboid(vec1, vec2);

        setField(creator, "firstPos", vec1);
        setField(creator, "world", world);

        final ILocation loc = UnitTestUtil.getLocation(vec2, world);

        // Not allowed, because no access to location
        Assertions.assertFalse(creator.provideSecondPos(loc));

        Mockito.doReturn(true).when(creator).playerHasAccessToLocation(Mockito.any());
        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any()))
               .thenReturn(OptionalInt.of(cuboid.getVolume() - 1));
        // Not allowed, because the selected area is too big.
        Assertions.assertFalse(creator.provideSecondPos(loc));
        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.area_too_big"));

        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any()))
               .thenReturn(OptionalInt.of(cuboid.getVolume() + 1));
        Mockito.doReturn(false).when(creator).playerHasAccessToCuboid(Mockito.any(), Mockito.any());
        // Not allowed, because no access to one or more blocks in the cuboid area.
        Assertions.assertFalse(creator.provideSecondPos(loc));

        Mockito.doReturn(true).when(creator).playerHasAccessToCuboid(Mockito.any(), Mockito.any());
        Assertions.assertTrue(creator.provideSecondPos(loc));
        Assertions.assertEquals(cuboid, creator.getCuboid());
    }

    @Test
    void testConfirmPrice()
    {
        final CreatorImpl creator = newCreator();

        Mockito.doNothing().when(creator).abort();

        final var procedure = Mockito.spy(creator.getProcedure());

        Assertions.assertTrue(creator.confirmPrice(false));
        Mockito.verify(player).sendMessage(UnitTestUtil.textArgumentMatcher("creator.base.error.creation_cancelled"));

        Mockito.doReturn(OptionalDouble.empty()).when(creator).getPrice();
        Mockito.doReturn(false).when(creator).buyStructure();

        Assertions.assertTrue(creator.confirmPrice(true));
        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.insufficient_funds"));

        final double price = 123.41;
        Mockito.doReturn(OptionalDouble.of(price)).when(creator).getPrice();
        Mockito.doReturn(false).when(creator).buyStructure();
        Assertions.assertTrue(creator.confirmPrice(true));
        Mockito.verify(player, Mockito.times(2)).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.insufficient_funds"));

        Mockito.doReturn(true).when(creator).buyStructure();

        // Get the lock manually because we do not use the regular procedure.
        creator.acquireInputLock();
        Assertions.assertTrue(creator.confirmPrice(true));
        creator.releaseInputLock();

        Mockito.verify(procedure).goToNextStep();
    }

    @Test
    void testSkipPrice()
    {
        final Creator creator = newCreator();

        Mockito.doReturn(OptionalDouble.empty()).when(creator).getPrice();
        Assertions.assertTrue(creator.skipConfirmPrice());

        Mockito.doReturn(OptionalDouble.of(1)).when(creator).getPrice();
        Assertions.assertFalse(creator.skipConfirmPrice());
    }

    @Test
    void testOpenDirectionStep()
    {
        final Creator creator = newCreator();

        final var setOpenDirectionDelayed = Mockito.mock(SetOpenDirectionDelayed.class);
        Mockito.when(setOpenDirectionDelayed.runDelayed(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(null));
        Mockito.when(commandFactory.getSetOpenDirectionDelayed()).thenReturn(setOpenDirectionDelayed);

        final StructureType structureType = Mockito.mock(StructureType.class);
        final Set<MovementDirection> validOpenDirections = EnumSet.of(MovementDirection.EAST, MovementDirection.WEST);
        Mockito.when(structureType.getValidOpenDirections()).thenReturn(validOpenDirections);

        Mockito.when(creator.getStructureType()).thenReturn(structureType);

        Assertions.assertFalse(creator.completeSetOpenDirStep(MovementDirection.NONE));
        Assertions.assertFalse(creator.completeSetOpenDirStep(MovementDirection.NORTH));
        Assertions.assertTrue(creator.completeSetOpenDirStep(MovementDirection.EAST));
        Assertions.assertFalse(creator.completeSetOpenDirStep(MovementDirection.SOUTH));
        Assertions.assertTrue(creator.completeSetOpenDirStep(MovementDirection.WEST));
        Assertions.assertFalse(creator.completeSetOpenDirStep(MovementDirection.CLOCKWISE));
        Assertions.assertFalse(creator.completeSetOpenDirStep(MovementDirection.COUNTERCLOCKWISE));
        Assertions.assertFalse(creator.completeSetOpenDirStep(MovementDirection.UP));
        Assertions.assertFalse(creator.completeSetOpenDirStep(MovementDirection.DOWN));
    }

    @Test
    void testGetPrice()
    {
        final Creator creator = newCreator();

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(false);
        final Cuboid cuboid = new Cuboid(new Vector3Di(1, 2, 3), new Vector3Di(4, 5, 6));
        setField(creator, "cuboid", cuboid);
        Assertions.assertTrue(creator.getPrice().isEmpty());

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        Mockito.when(economyManager.getPrice(Mockito.any(), Mockito.anyInt()))
               .thenAnswer(invocation -> OptionalDouble.of(invocation.getArgument(1, Integer.class).doubleValue()));

        final OptionalDouble price = creator.getPrice();
        Assertions.assertTrue(price.isPresent());
        Assertions.assertEquals(cuboid.getVolume(), price.getAsDouble());
    }

    @Test
    void testBuyStructure()
    {
        final Creator creator = newCreator();

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(false);

        final Cuboid cuboid = new Cuboid(new Vector3Di(1, 2, 3), new Vector3Di(4, 5, 6));
        setField(creator, "cuboid", cuboid);
        Assertions.assertTrue(creator.buyStructure());

        final IWorld world = Mockito.mock(IWorld.class);
        setField(creator, "world", world);

        final StructureType StructureType = Mockito.mock(StructureType.class);
        Mockito.when(creator.getStructureType()).thenReturn(StructureType);

        Mockito.when(economyManager.isEconomyEnabled()).thenReturn(true);
        creator.buyStructure();
        Mockito.verify(economyManager).buyStructure(player, world, StructureType, cuboid.getVolume());
    }

    @Test
    void testCompleteSetPowerBlockStep()
    {
        final Creator creator = newCreator();

        Mockito.doNothing().when(creator).abort();

        final IWorld world = UnitTestUtil.getWorld();

        final Vector3Di cuboidMin = new Vector3Di(10, 20, 30);
        final Vector3Di cuboidMax = new Vector3Di(40, 50, 60);
        final Cuboid cuboid = new Cuboid(cuboidMin, cuboidMax);

        final ILocation outsideCuboid = UnitTestUtil.getLocation(70, 80, 90, world);
        final ILocation insideCuboid = UnitTestUtil.getLocation(25, 35, 45, world);

        setField(creator, "cuboid", cuboid);
        setField(creator, "world", world);

        Assertions.assertFalse(creator.completeSetPowerBlockStep(UnitTestUtil.getLocation(0, 1, 2)));

        Mockito.doReturn(false).when(creator).playerHasAccessToLocation(Mockito.any());
        Assertions.assertFalse(creator.completeSetPowerBlockStep(outsideCuboid));

        Mockito.doReturn(true).when(creator).playerHasAccessToLocation(Mockito.any());
        Assertions.assertFalse(creator.completeSetPowerBlockStep(insideCuboid));

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.powerblock_inside_structure"));

        final double distance = cuboid.getCenter().getDistance(outsideCuboid.getPosition());
        final int lowLimit = (int) (distance - 1);
        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any())).thenReturn(OptionalInt.of(lowLimit));

        Assertions.assertFalse(creator.completeSetPowerBlockStep(outsideCuboid));
        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.powerblock_too_far"));

        Mockito.when(limitsManager.getLimit(Mockito.any(), Mockito.any())).thenReturn(OptionalInt.of(lowLimit + 10));
        Assertions.assertTrue(creator.completeSetPowerBlockStep(outsideCuboid));
    }

    @Test
    void testCompleteSetRotationPointStep()
    {
        final Creator creator = newCreator();

        final IWorld world = UnitTestUtil.getWorld();

        final Vector3Di cuboidMin = new Vector3Di(10, 20, 30);
        final Vector3Di cuboidMax = new Vector3Di(40, 50, 60);
        final Cuboid cuboid = new Cuboid(cuboidMin, cuboidMax);

        setField(creator, "world", world);
        setField(creator, "cuboid", cuboid);

        // World mismatch, so not allowed
        Assertions.assertFalse(creator.completeSetRotationPointStep(UnitTestUtil.getLocation(1, 1, 1)));

        Mockito.doReturn(false).when(creator).playerHasAccessToLocation(Mockito.any());
        // Location not allowed
        Assertions.assertFalse(creator.completeSetRotationPointStep(UnitTestUtil.getLocation(1, 1, 1, world)));

        Mockito.doReturn(true).when(creator).playerHasAccessToLocation(Mockito.any());
        // Point too far away
        Assertions.assertFalse(creator.completeSetRotationPointStep(UnitTestUtil.getLocation(1, 1, 1, world)));
        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("creator.base.error.invalid_rotation_point"));

        Assertions.assertTrue(creator.completeSetRotationPointStep(UnitTestUtil.getLocation(11, 21, 31, world)));
    }

    private Step newDefaultStep()
    {
        try
        {
            return context
                .getStepFactory()
                .stepName("test")
                .stepExecutor(Mockito.mock(StepExecutor.class))
                .textSupplier(text -> text.append("test"))
                .construct();
        }
        catch (InstantiationException e)
        {
            throw new RuntimeException(e);
        }
    }

    private CreatorImpl newCreator()
    {
        return newCreator(null);
    }

    private CreatorImpl newCreator(@Nullable List<Step> steps)
    {
        final List<Step> stepsList = steps == null ? List.of(newDefaultStep()) : steps;
        final CreatorImpl creator = new CreatorImpl(context, player, structureType, stepsList, null);
        return Mockito.spy(creator);
    }

    private void setField(Creator creator, String fieldName, @Nullable Object obj)
    {
        try
        {
            final Field f = Creator.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(creator, obj);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static final class CreatorImpl extends Creator
    {
        private final StructureType structureType;
        private final List<Step> steps;
        private final Procedure procedure;

        private final Method acquireInputLock;
        private final Method releaseInputLock;

        public CreatorImpl(
            ToolUser.Context context,
            IPlayer player,
            StructureType structureType,
            List<Step> steps,
            @Nullable String name)
        {
            super(context, player, name);
            this.structureType = structureType;
            this.steps = List.copyOf(steps);
            this.procedure = findProcedure();
            this.acquireInputLock = ReflectionUtil.getMethod(ToolUser.class, "acquireInputLock");
            this.releaseInputLock = ReflectionUtil.getMethod(ToolUser.class, "releaseInputLock");
        }

        public void acquireInputLock()
        {
            try
            {
                acquireInputLock.invoke(this);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        public void releaseInputLock()
        {
            try
            {
                releaseInputLock.invoke(this);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void giveTool()
        {
            // Do nothing
        }

        @Override
        protected AbstractStructure constructStructure()
        {
            throw new UnsupportedOperationException("No implemented!");
        }

        @Override
        protected StructureType getStructureType()
        {
            return structureType;
        }

        @Override
        protected List<Step> generateSteps()
            throws InstantiationException
        {
            return this.steps;
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

        public Procedure getProcedure()
        {
            return procedure;
        }
    }
}
