package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.movable.AbstractMovable;
import nl.pim16aap2.bigdoors.movabletypes.MovableType;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetriever;
import nl.pim16aap2.bigdoors.util.movableretriever.MovableRetrieverFactory;
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

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.movableOwnerCreator;

class DeleteTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock
    private DatabaseManager databaseManager;

    private MovableRetriever doorRetriever;

    @Mock
    private AbstractMovable door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private Delete.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);

        final MovableType doorType = Mockito.mock(MovableType.class);
        Mockito.when(doorType.getLocalizationKey()).thenReturn("DoorType");
        Mockito.when(door.getMovableType()).thenReturn(doorType);

        Mockito.when(door.isOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(door.isOwner(Mockito.any(IPPlayer.class))).thenReturn(true);
        doorRetriever = MovableRetrieverFactory.ofMovable(door);

        Mockito.when(databaseManager.deleteMovable(Mockito.any(), Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newDelete(Mockito.any(ICommandSender.class),
                                       Mockito.any(MovableRetriever.class)))
               .thenAnswer(invoc -> new Delete(invoc.getArgument(0, ICommandSender.class), localizer,
                                               ITextFactory.getSimpleTextFactory(),
                                               invoc.getArgument(1, MovableRetriever.class),
                                               databaseManager));
    }

    @Test
    void testServer()
        throws Exception
    {
        final IPServer server = Mockito.mock(IPServer.class, Answers.CALLS_REAL_METHODS);
        Assertions.assertTrue(factory.newDelete(server, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager).deleteMovable(door, null);
    }

    @Test
    void testExecution()
        throws Exception
    {
        // No permissions, so not allowed.
        initCommandSenderPermissions(commandSender, false, false);
        Assertions.assertTrue(factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.never()).deleteMovable(door, commandSender);

        // Has user permission, but not an owner, so not allowed.
        initCommandSenderPermissions(commandSender, true, false);
        Assertions.assertTrue(factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.never()).deleteMovable(door, commandSender);

        // Has user permission, and is owner, so allowed.
        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.of(movableOwnerCreator));
        Assertions.assertTrue(factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1)).deleteMovable(door, commandSender);

        // Admin permission, so allowed, despite not being owner.
        Mockito.when(door.getOwner(commandSender)).thenReturn(Optional.empty());
        initCommandSenderPermissions(commandSender, true, true);
        Assertions.assertTrue(factory.newDelete(commandSender, doorRetriever).run().get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(2)).deleteMovable(door, commandSender);
    }
}
