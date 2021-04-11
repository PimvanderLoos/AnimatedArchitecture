package nl.pim16aap2.bigdoors.commands;

import nl.pim16aap2.bigdoors.BigDoors;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.api.PPlayerData;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
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

class AddOwnerTest
{
    @Mock
    private IBigDoorsPlatform platform;

    @Mock
    private DoorRetriever doorRetriever;

    @Mock
    private AbstractDoorBase door;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer target;

    private static final PPlayerData playerData = Mockito.mock(PPlayerData.class);
    private static final DoorOwner doorOwner0 = new DoorOwner(0, 0, playerData);
    private static final DoorOwner doorOwner1 = new DoorOwner(0, 1, playerData);
    private static final DoorOwner doorOwner2 = new DoorOwner(0, 2, playerData);

    private AddOwner addOwner0;
    private AddOwner addOwner1;
    private AddOwner addOwner2;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);
        BigDoors.get().setBigDoorsPlatform(platform);
        Mockito.when(platform.getPLogger()).thenReturn(new BasicPLogger());

        Mockito.when(commandSender.hasPermission(Mockito.any(String.class)))
               .thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(commandSender.hasPermission(Mockito.any(CommandDefinition.class)))
               .thenReturn(CompletableFuture.completedFuture(new BooleanPair(true, true)));

        Mockito.when(doorRetriever.getDoor()).thenReturn(CompletableFuture.completedFuture(Optional.of(door)));
        Mockito.when(doorRetriever.getDoor(Mockito.any()))
               .thenReturn(CompletableFuture.completedFuture(Optional.of(door)));

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
}
