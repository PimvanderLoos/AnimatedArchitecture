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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommanTestingUtil.*;

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

    @Mock
    DoorOpener doorOpener;

    @Captor ArgumentCaptor<AbstractDoorBase> captureDoorOpenerDoor;
    @Captor ArgumentCaptor<DoorActionCause> captureDoorOpenerCause;
    @Captor ArgumentCaptor<IPPlayer> captureDoorOpenerResponsible;
    @Captor ArgumentCaptor<Double> captureDoorOpenerTime;
    @Captor ArgumentCaptor<Boolean> captureDoorOpenerSkip;
    @Captor ArgumentCaptor<DoorActionType> captureDoorOpenerActionType;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

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

    private void verifyDoorOpenerCall(AbstractDoorBase door, DoorActionCause doorActionCause,
                                      ICommandSender commandSender, double time, boolean skip,
                                      DoorActionType doorActionType)
    {
        Mockito.verify(doorOpener).animateDoorAsync(captureDoorOpenerDoor.capture(),
                                                    captureDoorOpenerCause.capture(),
                                                    captureDoorOpenerResponsible.capture(),
                                                    captureDoorOpenerTime.capture(),
                                                    captureDoorOpenerSkip.capture(),
                                                    captureDoorOpenerActionType.capture());

        Assertions.assertEquals(door, captureDoorOpenerDoor.getValue());
        Assertions.assertEquals(doorActionCause, captureDoorOpenerCause.getValue());
        Assertions.assertEquals(commandSender, captureDoorOpenerResponsible.getValue());
        Assertions.assertEquals(time, captureDoorOpenerTime.getValue());
        Assertions.assertEquals(skip, captureDoorOpenerSkip.getValue());
        Assertions.assertEquals(doorActionType, captureDoorOpenerActionType.getValue());
    }

    @Test
    @SneakyThrows
    void testSuccess()
    {
        Toggle toggle = new Toggle(commandSender, doorRetriever);
        Assertions.assertTrue(toggle.executeCommand(new BooleanPair(true, true)).get(1, TimeUnit.SECONDS));
        verifyDoorOpenerCall(door, DoorActionCause.PLAYER, commandSender, 0.0D, false, DoorActionType.TOGGLE);
    }
}
