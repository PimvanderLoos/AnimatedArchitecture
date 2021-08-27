package nl.pim16aap2.bigdoors.tooluser;

import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.util.vector.Vector3Di;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;

class PowerBlockRelocatorTest
{
    @Mock
    private AbstractDoor door;

    @Mock
    private IPWorld world;

    private final Vector3Di currentPowerBlockLoc = new Vector3Di(2, 58, 2384);

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer player;

    private IProtectionCompatManager compatManager;

    @Mock
    private IPLocation location;

    @BeforeEach
    void init()
    {
        final var platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        Mockito.when(door.getWorld()).thenReturn(world);
        Mockito.when(door.getPowerBlock()).thenReturn(currentPowerBlockLoc);

        compatManager = Mockito.mock(IProtectionCompatManager.class);
        Mockito.when(compatManager.canBreakBlock(Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(compatManager.canBreakBlocksBetweenLocs(Mockito.any(), Mockito.any(),
                                                             Mockito.any(), Mockito.any()))
               .thenReturn(Optional.empty());

        Mockito.when(platform.getProtectionCompatManager()).thenReturn(compatManager);
        Mockito.when(platform.getToolUserManager()).thenReturn(Mockito.mock(ToolUserManager.class));
        Mockito.when(platform.getBigDoorsToolUtil()).thenReturn(Mockito.mock(IBigDoorsToolUtil.class));
    }

    @Test
    void testMoveToLocWorld()
    {
        final var relocator = new PowerBlockRelocator(player, door);

        Mockito.when(location.getWorld()).thenReturn(Mockito.mock(IPWorld.class));

        Assertions.assertFalse(relocator.moveToLoc(location));
        Mockito.verify(player).sendMessage("tool_user.powerblock_relocator.error.world_mismatch");

        Mockito.when(location.getWorld()).thenReturn(Mockito.mock(IPWorld.class));
    }

    @Test
    void testMoveToLocDuplicated()
    {
        final var relocator = new PowerBlockRelocator(player, door);

        Mockito.when(location.getWorld()).thenReturn(world);

        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));
        Assertions.assertTrue(relocator.moveToLoc(location));

        Mockito.when(location.getPosition()).thenReturn(currentPowerBlockLoc);
        Assertions.assertTrue(relocator.moveToLoc(location));
    }

    @Test
    void testMoveToLocNoAccess()
    {
        final var relocator = new PowerBlockRelocator(player, door);

        final var compat = "TestCompat";
        Mockito.when(compatManager.canBreakBlock(Mockito.any(), Mockito.any())).thenReturn(Optional.of(compat));

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));

        Assertions.assertFalse(relocator.moveToLoc(location));
    }

    @Test
    void testExecution()
    {
        final var relocator = new PowerBlockRelocator(player, door);

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));

        Assertions.assertTrue(relocator.handleInput(location));

        Mockito.verify(door).syncData();
    }

    @Test
    void testExecutionUnchanged()
    {
        final var relocator = new PowerBlockRelocator(player, door);

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(currentPowerBlockLoc);

        Assertions.assertTrue(relocator.handleInput(location));

        Mockito.verify(door, Mockito.never()).syncData();
    }
}
