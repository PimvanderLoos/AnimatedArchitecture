package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.factories.ITextFactory;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.PermissionLevel;
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
import org.mockito.stubbing.Answer;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class AddOwnerTest
{
    private ILocalizer localizer;

    @Mock
    private DatabaseManager databaseManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AddOwner.IFactory factory;

    private DoorRetriever doorRetriever;

    @Mock
    private AbstractDoor door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer target;

    private AddOwner addOwnerCreator;
    private AddOwner addOwnerAdmin;
    private AddOwner addOwnerUser;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        localizer = UnitTestUtil.initLocalizer();

        Mockito.when(databaseManager.addOwner(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        Mockito.when(factory.newAddOwner(Mockito.any(ICommandSender.class),
                                         Mockito.any(DoorRetriever.class),
                                         Mockito.any(IPPlayer.class),
                                         Mockito.any(PermissionLevel.class)))
               .thenAnswer((Answer<AddOwner>) invoc ->
                   new AddOwner(invoc.getArgument(0, ICommandSender.class), localizer,
                                ITextFactory.getSimpleTextFactory(),
                                invoc.getArgument(1, DoorRetriever.class),
                                invoc.getArgument(2, IPPlayer.class), invoc.getArgument(3, PermissionLevel.class),
                                databaseManager));

        initCommandSenderPermissions(commandSender, true, true);
        doorRetriever = DoorRetrieverFactory.ofDoor(door);

        addOwnerCreator = factory.newAddOwner(commandSender, doorRetriever, target, PermissionLevel.CREATOR);
        addOwnerAdmin = factory.newAddOwner(commandSender, doorRetriever, target, PermissionLevel.ADMIN);
        addOwnerUser = factory.newAddOwner(commandSender, doorRetriever, target, PermissionLevel.USER);
    }

    @Test
    void testInputValidity()
    {
        Assertions.assertFalse(addOwnerCreator.validInput());
        Assertions.assertTrue(addOwnerAdmin.validInput());
        Assertions.assertTrue(addOwnerUser.validInput());
        Assertions.assertFalse(
            factory.newAddOwner(commandSender, doorRetriever, target, PermissionLevel.NO_PERMISSION).validInput());
    }

    @Test
    void testIsAllowed()
    {
        Assertions.assertTrue(addOwnerAdmin.isAllowed(door, true));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwnerCreator));
        Assertions.assertFalse(addOwnerCreator.isAllowed(door, false));
        Assertions.assertTrue(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertTrue(addOwnerUser.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwnerAdmin));
        Assertions.assertFalse(addOwnerCreator.isAllowed(door, false));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertTrue(addOwnerUser.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwnerUser));
        Assertions.assertFalse(addOwnerCreator.isAllowed(door, false));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertFalse(addOwnerUser.isAllowed(door, false));
    }

    @Test
    void nonPlayer()
    {
        final ICommandSender server = Mockito.mock(ICommandSender.class, Answers.CALLS_REAL_METHODS);
        final AddOwner addOwner = factory.newAddOwner(server, doorRetriever, target, PermissionLevel.CREATOR);

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwnerCreator));
        Assertions.assertFalse(addOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwnerAdmin));
        Assertions.assertTrue(addOwner.isAllowed(door, false));
    }

    @Test
    void testIsAllowedExistingTarget()
    {
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwnerCreator));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwnerAdmin));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertTrue(addOwnerUser.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwnerAdmin));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwnerAdmin));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertFalse(addOwnerUser.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwnerCreator));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwnerUser));
        Assertions.assertTrue(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertFalse(addOwnerUser.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwnerAdmin));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwnerCreator));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, false));
        Assertions.assertFalse(addOwnerUser.isAllowed(door, false));

        // It should never be possible to re-assign level 0 ownership, even with bypass enabled.
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwnerCreator));
        Assertions.assertFalse(addOwnerAdmin.isAllowed(door, true));
        Assertions.assertFalse(addOwnerUser.isAllowed(door, true));
    }

    // TODO: Re-implement this.
//    @Test
//    @SneakyThrows
//    void testDelayedInput()
//    {
//        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwnerCreator));
//        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwnerAdmin));
//
//        final int first = AddOwner.runDelayed(commandSender, doorRetriever);
//        final int second = AddOwner.provideDelayedInput(commandSender, target);
//
//        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
//        Assertions.assertEquals(first, second);
//
//        Mockito.verify(databaseManager, Mockito.times(1)).addOwner(door, target, AddOwner.DEFAULT_PERMISSION_LEVEL,
//                                                                   commandSender.getPlayer().orElse(null));
//    }

    @Test
    @SneakyThrows
    void testDatabaseInteraction()
    {
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwnerCreator));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwnerAdmin));
        Mockito.when(door.isDoorOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(door.isDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        final CompletableFuture<Boolean> result =
            factory.newAddOwner(commandSender, doorRetriever, target, AddOwner.DEFAULT_PERMISSION_LEVEL).run();

        Assertions.assertTrue(result.get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1)).addOwner(door, target, AddOwner.DEFAULT_PERMISSION_LEVEL,
                                                                   commandSender.getPlayer().orElse(null));
    }
}
