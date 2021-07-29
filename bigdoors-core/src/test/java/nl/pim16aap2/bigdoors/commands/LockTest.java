package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.IBigDoorsEventFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.events.IDoorPrepareLockChangeEvent;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
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

class LockTest
{
    private IBigDoorsPlatform platform;

    @Mock
    private DoorRetriever doorRetriever;

    @Mock
    private AbstractDoor door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private IDoorPrepareLockChangeEvent event;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        initDoorRetriever(doorRetriever, door);

        Mockito.when(door.syncData()).thenReturn(CompletableFuture.completedFuture(true));

        val factory = Mockito.mock(IBigDoorsEventFactory.class);
        Mockito.when(factory.createDoorPrepareLockChangeEvent(Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
               .thenReturn(event);
        Mockito.when(platform.getBigDoorsEventFactory()).thenReturn(factory);
    }

    @Test
    @SneakyThrows
    void test()
    {
        final boolean lock = true;
        Mockito.when(event.isCancelled()).thenReturn(true);

        Assertions.assertTrue(Lock.run(commandSender, doorRetriever, lock).get(1, TimeUnit.SECONDS));
        Mockito.verify(door, Mockito.never()).setLocked(lock);

        Mockito.when(event.isCancelled()).thenReturn(false);
        Assertions.assertTrue(Lock.run(commandSender, doorRetriever, lock).get(1, TimeUnit.SECONDS));
        Mockito.verify(door).setLocked(lock);
    }
}
