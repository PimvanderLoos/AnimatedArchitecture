package nl.pim16aap2.bigdoors.commands;

import lombok.Getter;
import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.ICommandSender;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoorBase;
import nl.pim16aap2.bigdoors.doors.DoorOpener;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionCause;
import nl.pim16aap2.bigdoors.events.dooraction.DoorActionType;
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
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.*;

class ToggleTest
{
    @Mock
    private DoorRetriever doorRetriever;

    @Getter
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    IBigDoorsPlatform platform;

    @Mock
    AbstractDoorBase door;

    DoorOpener doorOpener;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        doorOpener = Mockito.mock(DoorOpener.class);
        Mockito.when(platform.getDoorOpener()).thenReturn(doorOpener);
        initCommandSenderPermissions(commandSender, true, true);
        initDoorRetriever(doorRetriever, door);
    }

    @Test
    void testValidInput()
    {
        Assertions.assertTrue(new Toggle(commandSender, doorRetriever).validInput());
        Assertions.assertFalse(new Toggle(commandSender, new DoorRetriever[0]).validInput());
    }

    private static void verifyDoorOpenerCall(int times, DoorOpener doorOpener, AbstractDoorBase door,
                                             DoorActionCause doorActionCause,
                                             ICommandSender commandSender, double time, boolean skip,
                                             DoorActionType doorActionType)
    {
        Mockito.verify(doorOpener, Mockito.times(times))
               .animateDoorAsync(door, doorActionCause, (IPPlayer) commandSender, time, skip, doorActionType);
    }

    private void verifyDoorOpenerNeverCalled()
    {
        Mockito.verify(doorOpener, Mockito.never())
               .animateDoorAsync(Mockito.any(AbstractDoorBase.class), Mockito.any(), Mockito.any(), Mockito.anyDouble(),
                                 Mockito.anyBoolean(), Mockito.any());
    }

    @Test
    @SneakyThrows
    void testSuccess()
    {
        DoorOpener doorOpener = Mockito.mock(DoorOpener.class);
        Mockito.when(platform.getDoorOpener()).thenReturn(doorOpener);

        Toggle toggle = new Toggle(commandSender, doorRetriever);
        toggle.executeCommand(new BooleanPair(true, true)).get(1, TimeUnit.SECONDS);
        verifyDoorOpenerCall(1, doorOpener,
                             door, DoorActionCause.PLAYER, commandSender, 0.0D, false, DoorActionType.TOGGLE);

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        toggle.executeCommand(new BooleanPair(true, false)).get(1, TimeUnit.SECONDS);
        verifyDoorOpenerCall(2, doorOpener,
                             door, DoorActionCause.PLAYER, commandSender, 0.0D, false, DoorActionType.TOGGLE);
    }

    @Test
    @SneakyThrows
    void testHasAccess()
    {
        Toggle toggle = new Toggle(commandSender, doorRetriever);

        Assertions.assertTrue(toggle.hasAccess(door, true));
        Assertions.assertFalse(toggle.hasAccess(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner3));
        Assertions.assertFalse(toggle.hasAccess(door, false));

        Mockito.when(door.getDoorOwner(commandSender)).thenReturn(Optional.of(doorOwner0));
        Assertions.assertTrue(toggle.hasAccess(door, false));
    }
}
