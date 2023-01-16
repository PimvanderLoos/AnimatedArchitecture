package nl.pim16aap2.bigdoors.tooluser;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.tooluser.step.Step;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

class PowerBlockRelocatorTest
{
    @Mock
    private AbstractMovable movable;

    @Mock
    private IPWorld world;

    private final Vector3Di currentPowerBlockLoc = new Vector3Di(2, 58, 2384);

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer player;

    private IProtectionCompatManager compatManager;

    @Mock
    private IPLocation location;

    @Mock
    private PowerBlockRelocator.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        Mockito.when(movable.getWorld()).thenReturn(world);
        Mockito.when(movable.getPowerBlock()).thenReturn(currentPowerBlockLoc);

        final MovableType movableTypeType = Mockito.mock(MovableType.class);
        Mockito.when(movable.getMovableType()).thenReturn(movableTypeType);
        Mockito.when(movableTypeType.getLocalizationKey()).thenReturn("MovableType");

        compatManager = Mockito.mock(IProtectionCompatManager.class);
        Mockito.when(compatManager.canBreakBlock(Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(compatManager.canBreakBlocksBetweenLocs(Mockito.any(), Mockito.any(),
                                                             Mockito.any(), Mockito.any()))
               .thenReturn(Optional.empty());

        final ToolUser.Context context = Mockito.mock(ToolUser.Context.class, Answers.RETURNS_MOCKS);
        Mockito.when(context.getProtectionCompatManager()).thenReturn(compatManager);
        final ILocalizer localizer = UnitTestUtil.initLocalizer();
        Mockito.when(context.getLocalizer()).thenReturn(localizer);
        Mockito.when(context.getTextFactory()).thenReturn(ITextFactory.getSimpleTextFactory());

        final Step.Factory.IFactory assistedStepFactory = Mockito.mock(Step.Factory.IFactory.class);
        //noinspection deprecation
        Mockito.when(assistedStepFactory.stepName(Mockito.anyString()))
               .thenAnswer(invocation -> new Step.Factory(localizer, invocation.getArgument(0, String.class)));
        Mockito.when(context.getStepFactory()).thenReturn(assistedStepFactory);

        Mockito.when(factory.create(Mockito.any(IPPlayer.class), Mockito.any(AbstractMovable.class)))
               .thenAnswer(invoc -> new PowerBlockRelocator(context, invoc.getArgument(0, IPPlayer.class),
                                                            invoc.getArgument(1, AbstractMovable.class)));
    }

    @Test
    void testMoveToLocWorld()
    {
        final PowerBlockRelocator relocator = factory.create(player, movable);

        Mockito.when(location.getWorld()).thenReturn(Mockito.mock(IPWorld.class));

        Assertions.assertFalse(relocator.moveToLoc(location));
        Mockito.verify(player)
               .sendMessage(UnitTestUtil.toText("tool_user.powerblock_relocator.error.world_mismatch MovableType"));

        Mockito.when(location.getWorld()).thenReturn(Mockito.mock(IPWorld.class));
    }

    @Test
    void testMoveToLocDuplicated()
    {
        final PowerBlockRelocator relocator = factory.create(player, movable);

        Mockito.when(location.getWorld()).thenReturn(world);

        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));
        Assertions.assertTrue(relocator.moveToLoc(location));

        Mockito.when(location.getPosition()).thenReturn(currentPowerBlockLoc);
        Assertions.assertTrue(relocator.moveToLoc(location));
    }

    @Test
    void testMoveToLocNoAccess()
    {
        final PowerBlockRelocator relocator = factory.create(player, movable);

        final String compat = "TestCompat";
        Mockito.when(compatManager.canBreakBlock(Mockito.any(), Mockito.any())).thenReturn(Optional.of(compat));

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));

        Assertions.assertFalse(relocator.moveToLoc(location));
    }

    @Test
    void testExecution()
    {
        final PowerBlockRelocator relocator = factory.create(player, movable);

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));

        Assertions.assertTrue(relocator.handleInput(location));

        Mockito.verify(movable).syncData();
    }

    @Test
    void testExecutionUnchanged()
    {
        final PowerBlockRelocator relocator = factory.create(player, movable);

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(currentPowerBlockLoc);

        Assertions.assertTrue(relocator.handleInput(location));

        Mockito.verify(movable, Mockito.never()).syncData();
    }
}
