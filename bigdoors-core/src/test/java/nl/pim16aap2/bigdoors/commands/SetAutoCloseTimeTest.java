package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import lombok.val;
import nl.pim16aap2.bigdoors.api.IBigDoorsPlatform;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.doorArchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.managers.DelayedCommandInputManager;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.UnitTestUtil.initPlatform;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;
import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initDoorRetriever;

class SetAutoCloseTimeTest
{
    private AbstractDoor door;

    private IBigDoorsPlatform platform;

    @Mock
    private DoorRetriever doorRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @BeforeEach
    void init()
    {
        platform = initPlatform();
        MockitoAnnotations.openMocks(this);

        door = Mockito.mock(AbstractDoor.class,
                            Mockito.withSettings().extraInterfaces(ITimerToggleable.class));
        Mockito.when(door.syncData()).thenReturn(CompletableFuture.completedFuture(true));

        initCommandSenderPermissions(commandSender, true, true);
        initDoorRetriever(doorRetriever, door);
        Mockito.when(platform.getDelayedCommandInputManager()).thenReturn(new DelayedCommandInputManager());
    }

    @Test
    @SneakyThrows
    void testDoorTypes()
    {
        final int autoCloseValue = 42;

        val command = new SetAutoCloseTime(commandSender, doorRetriever, autoCloseValue);
        val altDoor = Mockito.mock(AbstractDoor.class);

        Assertions.assertTrue(command.performAction(altDoor).get(1, TimeUnit.SECONDS));
        Mockito.verify(altDoor, Mockito.never()).syncData();

        Assertions.assertTrue(command.performAction(door).get(1, TimeUnit.SECONDS));
        Mockito.verify((ITimerToggleable) door).setAutoCloseTime(autoCloseValue);
        Mockito.verify(door).syncData();
    }

    @Test
    @SneakyThrows
    void testStaticRunners()
    {
        final int autoCloseValue = 42;
        Assertions.assertTrue(SetAutoCloseTime.run(commandSender, doorRetriever, autoCloseValue)
                                              .get(1, TimeUnit.SECONDS));

        Mockito.verify((ITimerToggleable) door).setAutoCloseTime(autoCloseValue);
        Mockito.verify(door).syncData();
    }


    @Test
    @SneakyThrows
    void testDelayedInput()
    {
        final int autoCloseValue = 42;
        Mockito.when(platform.getLocalizer()).thenReturn(Mockito.mock(ILocalizer.class));

        val first = SetAutoCloseTime.runDelayed(commandSender, doorRetriever);
        val second = SetAutoCloseTime.provideDelayedInput(commandSender, autoCloseValue);

        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
        Assertions.assertEquals(first, second);

        Mockito.verify((ITimerToggleable) door).setAutoCloseTime(autoCloseValue);
        Mockito.verify(door).syncData();
    }
}
