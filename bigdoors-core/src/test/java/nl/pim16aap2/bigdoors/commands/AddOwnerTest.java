package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.managers.DatabaseManager;
import nl.pim16aap2.bigdoors.util.DoorOwner;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.pair.BooleanPair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class AddOwnerTest
{
    @Mock
    IBigDoorsPlatform platform;

    @Mock
    DoorRetriever doorRetriever;

    @Mock
    AbstractDoorBase door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    IPPlayer target;

    @Mock
    DatabaseManager databaseManager;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());

        Mockito.when(databaseManager.addOwner(Mockito.any(), Mockito.any(), Mockito.anyInt()))
               .thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(platform.getDatabaseManager()).thenReturn(databaseManager);

        Mockito.when(commandSender.hasPermission(Mockito.any(String.class)))
               .thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(commandSender.hasPermission(Mockito.any(CommandDefinition.class)))
               .thenReturn(CompletableFuture.completedFuture(new BooleanPair(true, true)));

        Mockito.when(doorRetriever.getDoor()).thenReturn(CompletableFuture.completedFuture(Optional.of(door)));
        Mockito.when(doorRetriever.getDoor(Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(Optional.of(door)));
    }

    @Test
    void testInputValidity()
    {
        Assertions.assertFalse(new AddOwner(commandSender, doorRetriever, target, 0).validInput());
        Assertions.assertTrue(new AddOwner(commandSender, doorRetriever, target, 1).validInput());
        Assertions.assertTrue(new AddOwner(commandSender, doorRetriever, target, 2).validInput());
        Assertions.assertFalse(new AddOwner(commandSender, doorRetriever, target, 3).validInput());
    }

    @Test
    void testIsAllowed()
    {
        Assertions.assertTrue(new AddOwner(commandSender, doorRetriever, target, 1).isAllowed(door, true));

        PPlayerData playerData = Mockito.mock(PPlayerData.class);
        DoorOwner doorOwner0 = new DoorOwner(0, 0, playerData);
        DoorOwner doorOwner1 = new DoorOwner(0, 1, playerData);
        DoorOwner doorOwner2 = new DoorOwner(0, 2, playerData);

        Mockito.when(door.getDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(Optional.of(doorOwner0));
        Assertions.assertFalse(new AddOwner(commandSender, doorRetriever, target, 0).isAllowed(door, false));
        Assertions.assertTrue(new AddOwner(commandSender, doorRetriever, target, 1).isAllowed(door, false));
        Assertions.assertTrue(new AddOwner(commandSender, doorRetriever, target, 2).isAllowed(door, false));

        Mockito.when(door.getDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(Optional.of(doorOwner1));
        Assertions.assertFalse(new AddOwner(commandSender, doorRetriever, target, 0).isAllowed(door, false));
        Assertions.assertFalse(new AddOwner(commandSender, doorRetriever, target, 1).isAllowed(door, false));
        Assertions.assertTrue(new AddOwner(commandSender, doorRetriever, target, 2).isAllowed(door, false));

        Mockito.when(door.getDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(Optional.of(doorOwner2));
        Assertions.assertFalse(new AddOwner(commandSender, doorRetriever, target, 0).isAllowed(door, false));
        Assertions.assertFalse(new AddOwner(commandSender, doorRetriever, target, 1).isAllowed(door, false));
        Assertions.assertFalse(new AddOwner(commandSender, doorRetriever, target, 2).isAllowed(door, false));
    }

    @Test
    @SneakyThrows
    void testExecutionSuccess()
    {
        Assertions.assertTrue(new AddOwner(commandSender, doorRetriever, target, 0)
                                  .executeCommand(new BooleanPair(true, true))
                                  .get(1, TimeUnit.SECONDS));
    }

    @Test
    @SneakyThrows
    void testExecutionFailureNoDoor()
    {
        Mockito.when(doorRetriever.getDoorInteractive(Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Assertions.assertFalse(new AddOwner(commandSender, doorRetriever, target, 0)
                                   .executeCommand(new BooleanPair(true, true))
                                   .get(1, TimeUnit.SECONDS));
    }

    @Test
    @SneakyThrows
    void testExecutionFailureDatabase()
    {
        Mockito.when(databaseManager.addOwner(Mockito.any(), Mockito.any(), Mockito.anyInt()))
               .thenThrow(new IllegalStateException("Generic Database Exception!"));
        ExecutionException exception = Assertions.assertThrows(ExecutionException.class, () ->
            new AddOwner(commandSender, doorRetriever, target, 0).executeCommand(new BooleanPair(true, true))
                                                                 .get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(IllegalStateException.class, exception.getCause().getClass());
    }
}
