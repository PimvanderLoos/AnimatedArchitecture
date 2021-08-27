package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class DeleteTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private DatabaseManager databaseManager;

    @Mock
    private DoorRetriever doorRetriever;

    @Mock
    private AbstractDoor door;

    @BeforeEach
    void init()
    {
        final var platform = UnitTestUtil.initPlatform();
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);

        initDoorRetriever(doorRetriever, door);

        Mockito.when(platform.getDatabaseManager()).thenReturn(databaseManager);

        Mockito.when(databaseManager.deleteDoor(Mockito.any(), Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        final var server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(Delete.run(server, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager).deleteDoor(door, null);
    }

    @Test
    @SneakyThrows
    void testExecution()
    {
        // No permissions, so not allowed.
        initCommandSenderPermissions(commandSender, false, false);
        Assertions.assertTrue(Delete.run(commandSender, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.never()).deleteDoor(door, commandSender);

        // Has user permission, but not an owner, so not allowed.
        initCommandSenderPermissions(commandSender, true, false);
        Assertions.assertTrue(Delete.run(commandSender, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.never()).deleteDoor(door, commandSender);

        // Has user permission, and is owner, so allowed.
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertTrue(Delete.run(commandSender, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1)).deleteDoor(door, commandSender);

        // Admin permission, so allowed, despite not being owner.
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.empty());
        initCommandSenderPermissions(commandSender, true, true);
        Assertions.assertTrue(Delete.run(commandSender, doorRetriever).get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(2)).deleteDoor(door, commandSender);
    }
}
