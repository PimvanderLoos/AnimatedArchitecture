package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.messages.Messages;
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
import java.util.logging.Level;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class AddOwnerTest
{
    private IBigDoorsPlatform platform;

    @Mock
    private DoorRetriever doorRetriever;

    @Mock
    private AbstractDoorBase door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer target;

    private AddOwner addOwner0;
    private AddOwner addOwner1;
    private AddOwner addOwner2;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        initCommandSenderPermissions(commandSender, true, true);
        initDoorRetriever(doorRetriever, door);

        addOwner0 = new AddOwner(commandSender, doorRetriever, target, 0);
        addOwner1 = new AddOwner(commandSender, doorRetriever, target, 1);
        addOwner2 = new AddOwner(commandSender, doorRetriever, target, 2);
    }

    @Test
    void testInputValidity()
    {
        Assertions.assertFalse(addOwner0.validInput());
        Assertions.assertTrue(addOwner1.validInput());
        Assertions.assertTrue(addOwner2.validInput());
        Assertions.assertFalse(new AddOwner(commandSender, doorRetriever, target, 3).validInput());
    }

    @Test
    void testIsAllowed()
    {
        Assertions.assertTrue(addOwner1.isAllowed(door, true));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertFalse(addOwner0.isAllowed(door, false));
        Assertions.assertTrue(addOwner1.isAllowed(door, false));
        Assertions.assertTrue(addOwner2.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(addOwner0.isAllowed(door, false));
        Assertions.assertFalse(addOwner1.isAllowed(door, false));
        Assertions.assertTrue(addOwner2.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertFalse(addOwner0.isAllowed(door, false));
        Assertions.assertFalse(addOwner1.isAllowed(door, false));
        Assertions.assertFalse(addOwner2.isAllowed(door, false));
    }

    @Test
    void testIsAllowedExistingTarget()
    {
        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(addOwner1.isAllowed(door, false));
        Assertions.assertTrue(addOwner2.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner1));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(addOwner1.isAllowed(door, false));
        Assertions.assertFalse(addOwner2.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner2));
        Assertions.assertTrue(addOwner1.isAllowed(door, false));
        Assertions.assertFalse(addOwner2.isAllowed(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner1));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertFalse(addOwner1.isAllowed(door, false));
        Assertions.assertFalse(addOwner2.isAllowed(door, false));

        // It should never be possible to re-assign level 0 ownership, even with bypass enabled.
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertFalse(addOwner1.isAllowed(door, true));
        Assertions.assertFalse(addOwner2.isAllowed(door, true));
    }

    @Test
    @SneakyThrows
    void testDelayedInput()
    {
        BigDoors.get().getPLogger().setConsoleLogLevel(Level.ALL);

        Mockito.when(platform.getMessages()).thenReturn(Mockito.mock(Messages.class));
        val databaseManager = Mockito.mock(DatabaseManager.class);
        Mockito.when(platform.getDatabaseManager()).thenReturn(databaseManager);

        Mockito.when(databaseManager.addOwner(Mockito.any(), Mockito.any(), Mockito.anyInt()))
               .thenReturn(CompletableFuture.completedFuture(DatabaseManager.ActionResult.SUCCESS));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Mockito.when(door.getDoorOwner(target)).thenReturn(Optional.of(doorOwner1));

        val first = AddOwner.runDelayed(commandSender, doorRetriever);
        val second = AddOwner.provideDelayedInput(commandSender, target);

        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(first, second);

        Mockito.verify(databaseManager, Mockito.times(1)).addOwner(door, target, AddOwner.DEFAULT_PERMISSION_LEVEL);
    }
}
