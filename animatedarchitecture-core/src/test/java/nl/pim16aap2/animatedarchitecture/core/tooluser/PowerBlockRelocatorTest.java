package nl.pim16aap2.animatedarchitecture.core.tooluser;

import nl.pim16aap2.animatedarchitecture.core.UnitTestUtil;
import nl.pim16aap2.animatedarchitecture.core.api.ILocation;
import nl.pim16aap2.animatedarchitecture.core.api.IPlayer;
import nl.pim16aap2.animatedarchitecture.core.api.IProtectionHookManager;
import nl.pim16aap2.animatedarchitecture.core.api.IWorld;
import nl.pim16aap2.animatedarchitecture.core.managers.DatabaseManager;
import nl.pim16aap2.animatedarchitecture.core.structures.Structure;
import nl.pim16aap2.animatedarchitecture.core.structures.StructureType;
import nl.pim16aap2.animatedarchitecture.core.util.vector.Vector3Di;
import nl.pim16aap2.testing.AssistedFactoryMocker;
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

import java.util.concurrent.CompletableFuture;

import static nl.pim16aap2.animatedarchitecture.core.UnitTestUtil.assertThatMessageable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(1)
class PowerBlockRelocatorTest
{
    @Mock
    private Structure structure;

    @Mock
    private IWorld world;

    private final Vector3Di currentPowerBlockLoc = new Vector3Di(2, 58, 2384);

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPlayer player;

    private IProtectionHookManager hookManager;

    @Mock
    private ILocation location;

    private AssistedFactoryMocker<PowerBlockRelocator, PowerBlockRelocator.IFactory> assistedFactory;

    @BeforeEach
    void init()
        throws NoSuchMethodException
    {
        when(structure.getWorld()).thenReturn(world);
        when(structure.getPowerBlock()).thenReturn(currentPowerBlockLoc);
        when(structure.syncData())
            .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final StructureType structureTypeType = mock(StructureType.class);
        when(structure.getType()).thenReturn(structureTypeType);
        when(structureTypeType.getLocalizationKey()).thenReturn("StructureType");

        hookManager = mock(IProtectionHookManager.class);
        when(hookManager
            .canBreakBlock(Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));
        when(hookManager
            .canBreakBlocksInCuboid(Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.allowed()));

        final ToolUser.Context context = mock(ToolUser.Context.class, Answers.RETURNS_MOCKS);
        when(context.getProtectionHookManager()).thenReturn(hookManager);

        final var assistedStepFactory =
            new AssistedFactoryMocker<>(Step.Factory.class, Step.Factory.IFactory.class).getFactory();

        when(context.getStepFactory()).thenReturn(assistedStepFactory);

        assistedFactory = new AssistedFactoryMocker<>(PowerBlockRelocator.class, PowerBlockRelocator.IFactory.class)
            .injectParameter(ToolUser.Context.class, context);
    }

    @Test
    void testMoveToLocWorld()
    {
        UnitTestUtil.initMessageable(player);
        final PowerBlockRelocator relocator = assistedFactory.getFactory().create(player, structure);

        when(location.getWorld()).thenReturn(mock(IWorld.class));

        assertFalse(relocator.moveToLoc(location).join());

        assertThatMessageable(player)
            .sentErrorMessage("tool_user.powerblock_relocator.error.world_mismatch");

        when(location.getWorld()).thenReturn(mock(IWorld.class));
    }

    @Test
    void testMoveToLocDuplicated()
    {
        UnitTestUtil.initMessageable(player);
        final PowerBlockRelocator relocator = assistedFactory.getFactory().create(player, structure);

        when(location.getWorld()).thenReturn(world);

        when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));
        assertTrue(relocator.moveToLoc(location).join());

        when(location.getPosition()).thenReturn(currentPowerBlockLoc);
        assertTrue(relocator.moveToLoc(location).join());
    }

    @Test
    void testMoveToLocNoAccess()
    {
        UnitTestUtil.initMessageable(player);
        final PowerBlockRelocator relocator = assistedFactory.getFactory().create(player, structure);

        final String compat = "TestCompat";
        when(hookManager
            .canBreakBlock(Mockito.any(), Mockito.any()))
            .thenReturn(CompletableFuture.completedFuture(IProtectionHookManager.HookCheckResult.denied(compat)));

        when(location.getWorld()).thenReturn(world);
        when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));

        assertFalse(relocator.moveToLoc(location).join());
    }

    @Test
    void testExecution()
    {
        UnitTestUtil.initMessageable(player);
        final PowerBlockRelocator relocator = assistedFactory.getFactory().create(player, structure);

        when(location.getWorld()).thenReturn(world);
        when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));

        assertTrue(relocator.handleInput(location).join());

        Mockito.verify(structure).syncData();
    }

    @Test
    void testExecutionUnchanged()
    {
        UnitTestUtil.initMessageable(player);
        final PowerBlockRelocator relocator = assistedFactory.getFactory().create(player, structure);

        when(location.getWorld()).thenReturn(world);
        when(location.getPosition()).thenReturn(currentPowerBlockLoc);

        assertTrue(relocator.handleInput(location).join());

        Mockito.verify(structure, Mockito.never()).syncData();
    }
}
