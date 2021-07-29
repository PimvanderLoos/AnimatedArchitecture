package nl.pim16aap2.bigdoors.tooluser;

import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsToolUtil;
import nl.pim16aap2.bigdoors.api.IPLocation;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.IPWorld;
import nl.pim16aap2.bigdoors.api.IProtectionCompatManager;
import nl.pim16aap2.bigdoors.doors.DoorBase;
import nl.pim16aap2.bigdoors.managers.ToolUserManager;
import nl.pim16aap2.bigdoors.util.messages.Message;
import nl.pim16aap2.bigdoors.util.messages.Messages;
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
    private DoorBase door;

    @Mock
    private IPWorld world;

    private final Vector3Di currentPowerBlockLoc = new Vector3Di(2, 58, 2384);

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer player;

    private Messages messages;

    private IProtectionCompatManager compatManager;

    @Mock
    private IPLocation location;

    @BeforeEach
    void init()
    {
        val platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        Mockito.when(door.getWorld()).thenReturn(world);
        Mockito.when(door.getPowerBlock()).thenReturn(currentPowerBlockLoc);
        Mockito.when(door.setPowerBlockPosition(Mockito.any())).thenReturn(door);

        messages = Mockito.mock(Messages.class);
        Mockito.when(messages.getString(Mockito.any())).thenReturn("A");
        Mockito.when(messages.getString(Mockito.any(), Mockito.any())).thenReturn("B");

        compatManager = Mockito.mock(IProtectionCompatManager.class);
        Mockito.when(compatManager.canBreakBlock(Mockito.any(), Mockito.any())).thenReturn(Optional.empty());
        Mockito.when(compatManager.canBreakBlocksBetweenLocs(Mockito.any(), Mockito.any(),
                                                             Mockito.any(), Mockito.any()))
               .thenReturn(Optional.empty());

        Mockito.when(platform.getProtectionCompatManager()).thenReturn(compatManager);
        Mockito.when(platform.getToolUserManager()).thenReturn(Mockito.mock(ToolUserManager.class));
        Mockito.when(platform.getMessages()).thenReturn(messages);
        Mockito.when(platform.getBigDoorsToolUtil()).thenReturn(Mockito.mock(IBigDoorsToolUtil.class));
    }

    @Test
    void testMoveToLocWorld()
    {
        val relocator = new PowerBlockRelocator(player, door);

        Mockito.when(messages.getString(Message.CREATOR_PBRELOCATOR_LOCATIONNOTINSAMEWORLD)).thenReturn("NOTINWORLD!");

        Mockito.when(location.getWorld()).thenReturn(Mockito.mock(IPWorld.class));

        Assertions.assertFalse(relocator.moveToLoc(location));
        Mockito.verify(player).sendMessage("NOTINWORLD!");

        Mockito.when(location.getWorld()).thenReturn(Mockito.mock(IPWorld.class));
    }

    @Test
    void testMoveToLocDuplicated()
    {
        val relocator = new PowerBlockRelocator(player, door);

        Mockito.when(location.getWorld()).thenReturn(world);

        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));
        Assertions.assertTrue(relocator.moveToLoc(location));

        Mockito.when(location.getPosition()).thenReturn(currentPowerBlockLoc);
        Assertions.assertTrue(relocator.moveToLoc(location));
    }

    @Test
    void testMoveToLocNoAccess()
    {
        val relocator = new PowerBlockRelocator(player, door);

        val compat = "TestCompat";
        Mockito.when(compatManager.canBreakBlock(Mockito.any(), Mockito.any())).thenReturn(Optional.of(compat));
        Mockito.when(messages.getString(Message.ERROR_NOPERMISSIONFORLOCATION, compat)).thenReturn(compat);

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));

        Assertions.assertFalse(relocator.moveToLoc(location));
        Mockito.verify(player).sendMessage(compat);
    }

    @Test
    void testExecution()
    {
        val relocator = new PowerBlockRelocator(player, door);

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(new Vector3Di(0, 0, 0));

        Assertions.assertTrue(relocator.handleInput(location));

        Mockito.verify(door).syncData();
    }

    @Test
    void testExecutionUnchanged()
    {
        val relocator = new PowerBlockRelocator(player, door);

        Mockito.when(location.getWorld()).thenReturn(world);
        Mockito.when(location.getPosition()).thenReturn(currentPowerBlockLoc);

        Assertions.assertTrue(relocator.handleInput(location));

        Mockito.verify(door, Mockito.never()).syncData();
    }
}
