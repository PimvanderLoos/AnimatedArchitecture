package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doors.doorarchetypes.ITimerToggleable;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class SetAutoCloseTimeTest
{
    private AbstractDoor door;

    private DoorRetriever.AbstractRetriever doorRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetAutoCloseTime.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        door = Mockito.mock(AbstractDoor.class,
                            Mockito.withSettings().extraInterfaces(ITimerToggleable.class));
        Mockito.when(door.syncData()).thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(door.isDoorOwner(Mockito.any(UUID.class))).thenReturn(true);
        Mockito.when(door.isDoorOwner(Mockito.any(IPPlayer.class))).thenReturn(true);

        initCommandSenderPermissions(commandSender, true, true);
        doorRetriever = DoorRetriever.ofDoor(door);

        final IPLogger logger = new BasicPLogger();
        final CompletableFutureHandler handler = new CompletableFutureHandler(logger);
        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetAutoCloseTime(Mockito.any(ICommandSender.class),
                                                 Mockito.any(DoorRetriever.AbstractRetriever.class),
                                                 Mockito.anyInt()))
               .thenAnswer(invoc -> new SetAutoCloseTime(invoc.getArgument(0, ICommandSender.class), logger, localizer,
                                                         invoc.getArgument(1, DoorRetriever.AbstractRetriever.class),
                                                         invoc.getArgument(2, Integer.class), handler));
    }

    @Test
    @SneakyThrows
    void testDoorTypes()
    {
        final int autoCloseValue = 42;

        final SetAutoCloseTime command = factory.newSetAutoCloseTime(commandSender, doorRetriever, autoCloseValue);
        final AbstractDoor altDoor = Mockito.mock(AbstractDoor.class);

        Assertions.assertTrue(command.performAction(altDoor).get(1, TimeUnit.SECONDS));
        Mockito.verify(altDoor, Mockito.never()).syncData();

        Assertions.assertTrue(command.performAction(door).get(1, TimeUnit.SECONDS));
        Mockito.verify((ITimerToggleable) door).setAutoCloseTime(autoCloseValue);
        Mockito.verify(door).syncData();
    }

    // TODO: Re-implement
//    @Test
//    @SneakyThrows
//    void testDelayedInput()
//    {
//        final int autoCloseValue = 42;
//
//        final int first = SetAutoCloseTime.runDelayed(commandSender, doorRetriever);
//        final int second = SetAutoCloseTime.provideDelayedInput(commandSender, autoCloseValue);
//
//        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
//        Assertions.assertEquals(first, second);
//
//        Mockito.verify((ITimerToggleable) door).setAutoCloseTime(autoCloseValue);
//        Mockito.verify(door).syncData();
//    }
}
