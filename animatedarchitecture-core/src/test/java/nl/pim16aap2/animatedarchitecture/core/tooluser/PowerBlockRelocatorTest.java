package nl.pim16aap2.animatedarchitecture.core.tooluser;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.api.factories.ITextFactory;
import nl.pim16aap2.animatedarchitecture.core.localization.ILocalizer;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.AbstractStructure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(1)
class PowerBlockRelocatorTest
{
    @Mock
    private AbstractStructure structure;

    @Mock
    private IWorld world;

    private final Vector3Di currentPowerBlockLoc = new Vector3Di(2, 58, 2384);

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer player;

    private IProtectionHookManager hookManager;

    @Mock
    private ILocation location;

    @Mock
    private PowerBlockRelocator.IFactory factory;

    @BeforeEach
    void init()
    {
        Mockito.when(structure.getWorld()).thenReturn(world);
        Mockito.when(structure.getPowerBlock()).thenReturn(currentPowerBlockLoc);
        Mockito.when(structure.syncData())
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final StructureType structureTypeType = Mockito.mock(StructureType.class);
        Mockito.when(structure.getType()).thenReturn(structureTypeType);
        Mockito.when(structureTypeType.getLocalizationKey()).thenReturn("StructureType");

        hookManager = Mockito.mock(IProtectionHookManager.class);
        Mockito.when(hookManager.canBreakBlock(Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        Mockito.when(hookManager.canBreakBlocksBetweenLocs(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        final ToolUser.Context context = Mockito.mock(ToolUser.Context.class, Answers.RETURNS_MOCKS);
        Mockito.when(context.getProtectionHookManager()).thenReturn(hookManager);
        Mockito.when(context.getLocalizer()).thenReturn(localizer);
        Mockito.when(context.getTextFactory()).thenReturn(ITextFactory.getSimpleTextFactory());

        final Step.Factory.IFactory assistedStepFactory = Mockito.mock(Step.Factory.IFactory.class);
        //noinspection deprecation
        Mockito.when(assistedStepFactory.stepName(Mockito.anyString()))
            .thenAnswer(invocation -> new Step.Factory(localizer, invocation.getArgument(0, String.class)));
        Mockito.when(context.getStepFactory()).thenReturn(assistedStepFactory);

        Mockito.when(factory.create(Mockito.any(IPlayer.class), Mockito.any(AbstractStructure.class)))
            .thenAnswer(invoc -> new PowerBlockRelocator(context, invoc.getArgument(0, IPlayer.class),
                invoc.getArgument(1, AbstractStructure.class)));
    }

    @Test
    void testMoveToLocWorld()
    {
        final PowerBlockRelocator relocator = factory.create(player, structure);

        Mockito.when(location.getWorld()).thenReturn(Mockito.mock(IWorld.class));

        Assertions.assertFalse(relocator.moveToLoc(location).join());

        Mockito.verify(player).sendMessage(
            UnitTestUtil.textArgumentMatcher("tool_user.powerblock_relocator.error.world_mismatch"));

        Mockito.when(location.getWorld()).thenReturn(Mockito.mock(IWorld.class));
    }

    @Test
    void testMoveToLocDuplicated()
    {
        final PowerBlockRelocator relocator = factory.create(player, structure);

        Mockito.when(location.getWorld()).thenReturn(world);

        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));
        Assertions.assertTrue(relocator.moveToLoc(location).join());

        Mockito.when(location.getPosition()).thenReturn(currentPowerBlockLoc);
        Assertions.assertTrue(relocator.moveToLoc(location).join());
    }

    @Test
    void testMoveToLocNoAccess()
    {
        final PowerBlockRelocator relocator = factory.create(player, structure);

        final String compat = "TestCompat";
        Mockito.when(hookManager.canBreakBlock(Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(compat)));

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));

        Assertions.assertFalse(relocator.moveToLoc(location).join());
    }

    @Test
    void testExecution()
    {
        final PowerBlockRelocator relocator = factory.create(player, structure);

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));

        Assertions.assertTrue(relocator.handleInput(location).join());

        Mockito.verify(structure).syncData();
    }

    @Test
    void testExecutionUnchanged()
    {
        final PowerBlockRelocator relocator = factory.create(player, structure);

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(currentPowerBlockLoc);

        Assertions.assertTrue(relocator.handleInput(location).join());

        Mockito.verify(structure, Mockito.never()).syncData();
    }
}
