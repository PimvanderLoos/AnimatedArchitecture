package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
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

class RemoveOwnerTest
{
    @Mock
    private DoorRetriever doorRetriever;

    @Mock
    private AbstractDoor door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer target;

    private RemoveOwner removeOwner;

    private IBigDoorsPlatform platform;

    @BeforeEach
    void beforeEach()
    {
        platform = UnitTestUtil.initPlatform();
        door = Mockito.mock(AbstractDoor.class);
        doorRetriever = Mockito.mock(DoorRetriever.class);

        initDoorRetriever(doorRetriever, door);

        Mockito.when(platform.getDelayedCommandInputManager()).thenReturn(new DelayedCommandInputManager());

        MockitoAnnotations.openMocks(this);
        initCommandSenderPermissions(commandSender, true, true);
        removeOwner = new RemoveOwner(commandSender, doorRetriever, target);
    }

    /**
     * Ensure that even with the bypass permission, certain invalid actions are not allowed. (e.g. removing the original
     * creator).
     */
    @Test
    void testBypassLimitations()
    {
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
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertTrue(removeOwner.isAllowed(door, false));
    }

    @Test
    void testIsAllowed()
    {
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

    @Test
    @SneakyThrows
    void testDelayedInput()
    {
        Mockito.when(doorRetriever.getDoor()).thenReturn(CompletableFuture.completedFuture(Optional.of(door)));
        Mockito.when(platform.getLocalizer()).thenReturn(Mockito.mock(ILocalizer.class));
        final var databaseManager = mockDatabaseManager();

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));

        final var first = RemoveOwner.runDelayed(commandSender, doorRetriever);
        final var second = RemoveOwner.provideDelayedInput(commandSender, target);

        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(first, second);

        Mockito.verify(databaseManager, Mockito.times(1)).removeOwner(door, target,
                                                                      commandSender.getPlayer().orElse(null));
    }

    @Test
    @SneakyThrows
    void testStaticRunners()
    {
        Mockito.when(doorRetriever.getDoor()).thenReturn(CompletableFuture.completedFuture(Optional.of(door)));
        final var databaseManager = mockDatabaseManager();
        Mockito.when(platform.getLocalizer()).thenReturn(Mockito.mock(ILocalizer.class));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));

        final var result = RemoveOwner.run(commandSender, doorRetriever, target);
        Assertions.assertTrue(result.get(1, TimeUnit.SECONDS));
        Mockito.verify(databaseManager, Mockito.times(1)).removeOwner(door, target,
                                                                      commandSender.getPlayer().orElse(null));
    }

    private DatabaseManager mockDatabaseManager()
    {
        final DatabaseManager databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(platform.getDatabaseManager()).thenReturn(databaseManager);

        Mockito.when(databaseManager.removeOwner(Mockito.any(AbstractDoor.class), Mockito.any(IPPlayer.class),
                                                 Mockito.any(IPPlayer.class)))
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));
        return databaseManager;
    }
}
