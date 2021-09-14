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

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class RemoveOwnerTest
{
    private DoorRetriever.AbstractRetriever doorRetriever;

    @Mock
    private AbstractDoor door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer target;

    @Mock
    private DatabaseManager databaseManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private RemoveOwner.IFactory factory;

    @BeforeEach
    void beforeEach()
    {
        MockitoAnnotations.openMocks(this);
        initCommandSenderPermissions(commandSender, true, true);

        Mockito.when(door.isDoorOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(door.isDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(true);
        doorRetriever = DoorRetriever.ofDoor(door);

        final IPLogger logger = new BasicPLogger();
        final CompletableFutureHandler handler = new CompletableFutureHandler(logger);
        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(databaseManager.removeOwner(Mockito.any(AbstractDoor.class), Mockito.any(IPPlayer.class),
                                                 Mockito.any(IPPlayer.class)))
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        Mockito.when(factory.newRemoveOwner(Mockito.any(ICommandSender.class),
                                            Mockito.any(DoorRetriever.AbstractRetriever.class),
                                            Mockito.any(IPPlayer.class)))
               .thenAnswer(invoc -> new RemoveOwner(invoc.getArgument(0, ICommandSender.class), logger, localizer,
                                                    invoc.getArgument(1, DoorRetriever.AbstractRetriever.class),
                                                    invoc.getArgument(2, IPPlayer.class), databaseManager, handler));
    }

    /**
     * Ensure that even with the bypass permission, certain invalid actions are not allowed. (e.g. removing the original
     * creator).
     */
    @Test
    void testBypassLimitations()
    {
        final RemoveOwner removeOwner = factory.newRemoveOwner(commandSender, doorRetriever, target);

        // The target is not an owner, so this should be false despite the bypass permission.
        Assertions.assertFalse(removeOwner.isAllowed(door, true));

        // Removing the level0 owner is not allowed even with bypass enabled!
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertFalse(removeOwner.isAllowed(door, true));

        // Removing level>0 owners IS allowed with bypass even if
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertTrue(removeOwner.isAllowed(door, true));
    }

    @Test
    void testSuccess()
    {
        final RemoveOwner removeOwner = factory.newRemoveOwner(commandSender, doorRetriever, target);

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));
    }

    @Test
    void testIsAllowed()
    {
        final RemoveOwner removeOwner = factory.newRemoveOwner(commandSender, doorRetriever, target);

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertFalse(removeOwner.isAllowed(door, true));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.empty());
        Assertions.assertFalse(removeOwner.isAllowed(door, false));
    }

    // TODO: Re-implement
//    @Test
//    @SneakyThrows
//    void testDelayedInput()
//    {
//        Mockito.when(doorRetriever.getDoor()).thenReturn(CompletableFuture.completedFuture(Optional.of(door)));
//        Mockito.when(platform.getLocalizer()).thenReturn(Mockito.mock(ILocalizer.class));
//        final int databaseManager = mockDatabaseManager();
//
//        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
//        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));
//
//        final int first = RemoveOwner.runDelayed(commandSender, doorRetriever);
//        final int second = RemoveOwner.provideDelayedInput(commandSender, target);
//
//        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
//        Assertions.assertEquals(first, second);
//
//        Mockito.verify(databaseManager, Mockito.times(1)).removeOwner(door, target,
//                                                                      commandSender.getPlayer().orElse(null));
//    }

    @Test
    @SneakyThrows
    void testDatabaseInteraction()
    {
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));

        final CompletableFuture<Boolean> result = factory.newRemoveOwner(commandSender, doorRetriever, target).run();
        Assertions.assertTrue(result.get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1)).removeOwner(door, target,
                                                                      commandSender.getPlayer().orElse(null));
    }
}
