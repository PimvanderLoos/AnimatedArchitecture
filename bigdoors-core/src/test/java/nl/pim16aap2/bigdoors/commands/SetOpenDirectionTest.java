package nl.pim16aap2.bigdoors.commands;

import lombok.SneakyThrows;
import nl.pim16aap2.bigdoors.UnitTestUtil;
import nl.pim16aap2.bigdoors.api.IPPlayer;
import nl.pim16aap2.bigdoors.doors.AbstractDoor;
import nl.pim16aap2.bigdoors.doortypes.DoorType;
import nl.pim16aap2.bigdoors.localization.ILocalizer;
import nl.pim16aap2.bigdoors.logging.BasicPLogger;
import nl.pim16aap2.bigdoors.logging.IPLogger;
import nl.pim16aap2.bigdoors.util.CompletableFutureHandler;
import nl.pim16aap2.bigdoors.util.DoorRetriever;
import nl.pim16aap2.bigdoors.util.RotateDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static nl.pim16aap2.bigdoors.commands.CommandTestingUtil.initCommandSenderPermissions;

class SetOpenDirectionTest
{
    @Mock
    private AbstractDoor door;

    @Mock
    private DoorType doorType;

    private DoorRetriever.AbstractRetriever doorRetriever;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IPPlayer commandSender;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SetOpenDirection.IFactory factory;

    @BeforeEach
    void init()
    {
        MockitoAnnotations.openMocks(this);

        Mockito.when(door.syncData()).thenReturn(CompletableFuture.completedFuture(true));
        Mockito.when(door.getDoorType()).thenReturn(doorType);

        initCommandSenderPermissions(commandSender, true, true);
        doorRetriever = DoorRetriever.ofDoor(door);

        final IPLogger logger = new BasicPLogger();
        final CompletableFutureHandler handler = new CompletableFutureHandler(logger);
        final ILocalizer localizer = UnitTestUtil.initLocalizer();

        Mockito.when(factory.newSetOpenDirection(Mockito.any(ICommandSender.class),
                                                 Mockito.any(DoorRetriever.AbstractRetriever.class),
                                                 Mockito.any(RotateDirection.class)))
               .thenAnswer(invoc -> new SetOpenDirection(invoc.getArgument(0, ICommandSender.class), logger, localizer,
                                                         invoc.getArgument(1, DoorRetriever.AbstractRetriever.class),
                                                         invoc.getArgument(2, RotateDirection.class), handler));
    }

    @Test
    @SneakyThrows
    void testOpenDirValidity()
    {
        final RotateDirection rotateDirection = RotateDirection.CLOCKWISE;

        Mockito.when(doorType.isValidOpenDirection(Mockito.any())).thenReturn(false);
        final var command = factory.newSetOpenDirection(commandSender, doorRetriever, rotateDirection);

        Assertions.assertTrue(command.performAction(door).get(1, TimeUnit.SECONDS));
        Mockito.verify(door, Mockito.never()).syncData();
        Mockito.verify(door, Mockito.never()).setOpenDir(rotateDirection);


        Mockito.when(doorType.isValidOpenDirection(rotateDirection)).thenReturn(true);
        Assertions.assertTrue(command.performAction(door).get(1, TimeUnit.SECONDS));
        Mockito.verify(door).setOpenDir(rotateDirection);
        Mockito.verify(door).syncData();
    }

    // TODO: Re-implement
//    @Test
//    @SneakyThrows
//    void testDelayedInput()
//    {
//        final RotateDirection rotateDirection = RotateDirection.CLOCKWISE;
//        Mockito.when(doorType.isValidOpenDirection(rotateDirection)).thenReturn(true);
//
//        final var first = SetOpenDirection.runDelayed(commandSender, doorRetriever);
//        final var second = SetOpenDirection.provideDelayedInput(commandSender, rotateDirection);
//
//        Assertions.assertTrue(first.get(1, TimeUnit.SECONDS));
//        Assertions.assertEquals(first, second);
//
//        Mockito.verify(door).setOpenDir(rotateDirection);
//        Mockito.verify(door).syncData();
//    }
}
