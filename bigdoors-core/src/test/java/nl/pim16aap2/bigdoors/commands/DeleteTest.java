package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.doorOwner0;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class DeleteTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private DatabaseManager databaseManager;

    private DoorRetriever.AbstractRetriever doorRetriever;

    @Mock
    private AbstractDoor door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Delete.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);

        Mockito.when(door.isDoorOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(door.isDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(true);
        doorRetriever = DoorRetriever.ofDoor(door);

        Mockito.when(databaseManager.deleteDoor(Mockito.any(), Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final IPLogger logger = new BasicPLogger();
        final ILocalizer localizer = UnitTestUtil.initLocalizer();
        final CompletableFutureHandler handler = new CompletableFutureHandler(logger);

        Mockito.when(factory.newDelete(Mockito.any(ICommandSender.class),
                                       Mockito.any(DoorRetriever.AbstractRetriever.class)))
               .thenAnswer(invoc -> new Delete(invoc.getArgument(0, ICommandSender.class), logger, localizer,
                                               invoc.getArgument(1, DoorRetriever.AbstractRetriever.class),
                                               databaseManager, handler));
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        final var server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(factory.newDelete(server, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager).deleteDoor(door, null);
    }

    @Test
    @SneakyThrows
    void testExecution()
    {
        // No permissions, so not allowed.
        initCommandSenderPermissions(commandSender, false, false);
        Assertions.assertTrue(factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.never()).deleteDoor(door, commandSender);

        // Has user permission, but not an owner, so not allowed.
        initCommandSenderPermissions(commandSender, true, false);
        Assertions.assertTrue(factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.never()).deleteDoor(door, commandSender);

        // Has user permission, and is owner, so allowed.
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertTrue(factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1)).deleteDoor(door, commandSender);

        // Admin permission, so allowed, despite not being owner.
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.empty());
        initCommandSenderPermissions(commandSender, true, true);
        Assertions.assertTrue(factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(2)).deleteDoor(door, commandSender);
    }
}
