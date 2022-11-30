package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetriever;
import nl.pim16aap2.bigdoors.util.doorretriever.DoorRetrieverFactory;
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

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.doorOwnerCreator;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class DeleteTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private DatabaseManager databaseManager;

    private DoorRetriever doorRetriever;

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
        doorRetriever = DoorRetrieverFactory.ofDoor(door);

        Mockito.when(databaseManager.deleteDoor(Mockito.any(), Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newDelete(Mockito.any(ICommandSender.class),
                                       Mockito.any(DoorRetriever.class)))
               .thenAnswer(invoc -> new Delete(invoc.getArgument(0, ICommandSender.class), localizer,
                                               invoc.getArgument(1, DoorRetriever.class),
                                               databaseManager));
    }

    @Test
    @SneakyThrows
    void testServer()
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
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
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwnerCreator));
        Assertions.assertTrue(factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1)).deleteDoor(door, commandSender);

        // Admin permission, so allowed, despite not being owner.
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.empty());
        initCommandSenderPermissions(commandSender, true, true);
        Assertions.assertTrue(factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(2)).deleteDoor(door, commandSender);
    }
}
