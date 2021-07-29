package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.IDiscreteMovement;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.messages.Messages;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initDoorRetriever;

class SetBlocksToMoveTest
{
    private AbstractDoor door;

    private IBigDoorsPlatform platform;

    @Mock
    private DoorRetriever doorRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        door = Mockito.mock(AbstractDoor.class,
                            Mockito.withSettings().extraInterfaces(IDiscreteMovement.class));
        Mockito.when(door.syncData()).thenReturn(CompletableFuture.completedFuture(true));

        initCommandSenderPermissions(commandSender, true, true);
        initDoorRetriever(doorRetriever, door);
        Mockito.when(platform.getDelayedCommandInputManager()).thenReturn(new DelayedCommandInputManager());
    }

    @Test
    @SneakyThrows
    void testDoorTypes()
    {
        final int blocksToMove = 42;

        val command = new SetBlocksToMove(commandSender, doorRetriever, blocksToMove);
        val altDoor = Mockito.mock(AbstractDoor.class);

        Assertions.assertTrue(command.performAction(altDoor).get(1, TimeUnit.SECONDS));
        Mockito.verify(altDoor, Mockito.never()).syncData();

        Assertions.assertTrue(command.performAction(door).get(1, TimeUnit.SECONDS));
        Mockito.verify((IDiscreteMovement) door).setBlocksToMove(blocksToMove);
        Mockito.verify(door).syncData();
    }

    @Test
    @SneakyThrows
    void testStaticRunners()
    {
        final int blocksToMove = 42;
        Assertions.assertTrue(SetBlocksToMove.run(commandSender, doorRetriever, blocksToMove)
                                             .get(1, TimeUnit.SECONDS));

        Mockito.verify((IDiscreteMovement) door).setBlocksToMove(blocksToMove);
        Mockito.verify(door).syncData();
    }

    @Test
    @SneakyThrows
    void testDelayedInput()
    {
        final int blocksToMove = 42;
        Mockito.when(platform.getMessages()).thenReturn(Mockito.mock(Messages.class));

        val first = SetBlocksToMove.runDelayed(commandSender, doorRetriever);
        val second = SetBlocksToMove.provideDelayedInput(commandSender, blocksToMove);

        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(first, second);

        Mockito.verify((IDiscreteMovement) door).setBlocksToMove(blocksToMove);
        Mockito.verify(door).syncData();
    }
}
